import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:geolocator/geolocator.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../core/app_state.dart';
import '../../core/models.dart';
import '../../widgets/common.dart';
import '../shared/settings_screen.dart';
import '../store/store_shell.dart' show StatTile;

/// The driver's app: go online, see offers live, take one order, pick it up, deliver it.
/// While delivering, the phone shares its location so the customer sees the driver move on the map.
class DriverShell extends StatefulWidget {
  const DriverShell({super.key});
  @override
  State<DriverShell> createState() => _DriverShellState();
}

class _DriverShellState extends State<DriverShell> {
  bool online = false;
  String vehicle = 'MOTORBIKE';
  List<OrderView> offers = [], current = [], history = [];
  Map<String, dynamic>? earnings;
  Object? error;
  bool loading = true;
  Timer? _gps;
  late final AppState _s;
  late final void Function(String, dynamic) _listener;

  @override
  void initState() {
    super.initState();
    _s = context.read<AppState>();
    _listener = (event, data) {
      if (event == 'offer') {
        HapticFeedback.mediumImpact();
        SystemSound.play(SystemSoundType.alert);
      }
      if (event == 'offer' || event == 'offer-taken' || event == 'order') _load();
    };
    _s.extraListeners.add(_listener);
    _load();
  }

  @override
  void dispose() {
    _s.extraListeners.remove(_listener);
    _gps?.cancel();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final st = Map<String, dynamic>.from(await _s.api.get('/driver/status'));
      online = st['online'] == true;
      vehicle = st['vehicle'] ?? 'MOTORBIKE';
      current = (await _s.api.get('/driver/orders?active=true') as List).map((o) => OrderView.fromJson(o)).toList();
      history = (await _s.api.get('/driver/orders?active=false') as List).map((o) => OrderView.fromJson(o)).toList();
      earnings = Map<String, dynamic>.from(await _s.api.get('/driver/earnings'));
      offers = online && current.isEmpty
          ? (await _s.api.get('/driver/offers') as List).map((o) => OrderView.fromJson(o)).toList()
          : [];
      error = null;
    } catch (e) {
      error = e;
    }
    loading = false;
    _syncGps();
    if (mounted) setState(() {});
  }

  /// Location every 20 s while online (the store and customer see the driver; the server keeps the last point).
  void _syncGps() {
    if (online && _gps == null) {
      _sendLocation();
      _gps = Timer.periodic(const Duration(seconds: 20), (_) => _sendLocation());
    } else if (!online) {
      _gps?.cancel();
      _gps = null;
    }
  }

  Future<void> _sendLocation() async {
    try {
      var perm = await Geolocator.checkPermission();
      if (perm == LocationPermission.denied) perm = await Geolocator.requestPermission();
      if (perm == LocationPermission.denied || perm == LocationPermission.deniedForever) return;
      final p = await Geolocator.getCurrentPosition(locationSettings: const LocationSettings(accuracy: LocationAccuracy.high));
      await _s.api.post('/driver/location', {'lat': p.latitude, 'lng': p.longitude});
    } catch (_) {
      // no GPS right now — try again next tick
    }
  }

  Future<void> _setOnline(bool v) async {
    final r = await busy(context, () => _s.api.post('/driver/status', {'online': v, 'vehicle': vehicle}));
    if (r != null) _load();
  }

  Future<void> _setVehicle(String v) async {
    setState(() => vehicle = v);
    await busy(context, () => _s.api.post('/driver/status', {'online': online, 'vehicle': v}));
    _load();
  }

  Future<void> _step(OrderView o, String action) async {
    final r = await busy(context, () => _s.api.post('/driver/orders/${o.code}/$action'));
    if (r != null) _load();
  }

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(
        title: Text(s.t('app')),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _load),
          IconButton(
              icon: const Icon(Icons.settings_outlined),
              onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const SettingsScreen()))),
        ],
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(padding: const EdgeInsets.all(16), children: [
                if (error != null) Text(s.errorText(error!), style: TextStyle(color: cs.error)),
                Card(
                  color: online ? cs.secondaryContainer : null,
                  child: SwitchListTile(
                    secondary: Icon(online ? Icons.wifi_tethering : Icons.wifi_tethering_off, color: online ? cs.secondary : null),
                    title: Text(online ? s.t('online') : s.t('offline'), style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 18)),
                    subtitle: online ? null : Text(s.t('goOnline')),
                    value: online,
                    onChanged: _setOnline,
                  ),
                ),
                const SizedBox(height: 10),
                Text(s.t('vehicle'), style: const TextStyle(fontWeight: FontWeight.w800)),
                const SizedBox(height: 6),
                Wrap(spacing: 8, children: [
                  for (final v in ['MOTORBIKE', 'TUKTUK', 'BICYCLE', 'CAR'])
                    ChoiceChip(label: Text(s.t('veh_$v')), selected: vehicle == v, onSelected: (_) => _setVehicle(v)),
                ]),
                if (earnings != null) ...[
                  const SizedBox(height: 12),
                  Row(children: [
                    Expanded(child: StatTile(Icons.payments, s.money((earnings!['feesToday'] as num).toDouble()), s.t('earnings'))),
                    const SizedBox(width: 10),
                    Expanded(child: StatTile(Icons.check_circle, '${earnings!['deliveredToday']}', s.t('deliveries'))),
                  ]),
                ],
                if (current.isNotEmpty) ...[
                  SectionTitle(s.t('current')),
                  for (final o in current) _currentCard(s, cs, o),
                ] else if (online) ...[
                  SectionTitle(s.t('offersTitle')),
                  if (offers.isEmpty) EmptyState(Icons.hourglass_empty, s.t('noOffers')),
                  for (final o in offers) _offerCard(s, cs, o),
                ],
                if (history.isNotEmpty) ...[
                  SectionTitle(s.t('history')),
                  for (final o in history.take(10))
                    ListTile(
                      leading: StatusChip(o.status),
                      title: Text(o.code),
                      subtitle: Text(timeText(context, o.createdAt)),
                      trailing: Text(s.money(o.deliveryFee)),
                    ),
                ],
              ]),
            ),
    );
  }

  String _from(AppState s, OrderView o) => o.store == null ? s.t('errand') : (s.ar ? o.store!.nameAr : o.store!.name);

  Widget _offerCard(AppState s, ColorScheme cs, OrderView o) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(children: [
              Icon(o.isErrand ? Icons.shopping_basket : Icons.storefront, color: cs.primary),
              const SizedBox(width: 8),
              Expanded(child: Text(_from(s, o), style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 16))),
              Text(s.money(o.deliveryFee), style: TextStyle(color: cs.secondary, fontWeight: FontWeight.w900, fontSize: 18)),
            ]),
            const SizedBox(height: 4),
            Text('${s.t('to')}: ${s.zoneName(o.zone)}${o.landmark == null ? '' : ' — ${o.landmark}'}'),
            if (o.isErrand) Text('🛒 ${o.errandDetails ?? ''}', maxLines: 2, overflow: TextOverflow.ellipsis),
            const SizedBox(height: 10),
            FilledButton.icon(onPressed: () => _step(o, 'take'), icon: const Icon(Icons.check), label: Text(s.t('take'))),
          ]),
        ),
      ),
    );
  }

  Widget _currentCard(AppState s, ColorScheme cs, OrderView o) {
    final cash = o.paymentMethod == 'CASH';
    final canPick = o.status == 'READY' || (o.isErrand && o.status == 'ACCEPTED');
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            Text(o.code, style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 17)),
            const Spacer(),
            StatusChip(o.status),
          ]),
          const SizedBox(height: 8),
          Text('${s.t('from')}: ${_from(s, o)}', style: const TextStyle(fontWeight: FontWeight.w700)),
          if (o.store?.lat != null)
            TextButton.icon(
              onPressed: () => launchUrl(Uri.parse('https://www.google.com/maps/search/?api=1&query=${o.store!.lat},${o.store!.lng}'), mode: LaunchMode.externalApplication),
              icon: const Icon(Icons.map_outlined),
              label: Text(s.t('navigate')),
            ),
          const Divider(height: 18),
          Text(s.t('to'), style: const TextStyle(fontWeight: FontWeight.w700)),
          AddressBlock(o, showPhone: true),
          if (o.lat != null)
            TextButton.icon(
              onPressed: () => launchUrl(Uri.parse('https://www.google.com/maps/search/?api=1&query=${o.lat},${o.lng}'), mode: LaunchMode.externalApplication),
              icon: const Icon(Icons.map_outlined),
              label: Text(s.t('navigate')),
            ),
          const Divider(height: 18),
          if (o.isErrand) ...[
            Text(s.t('shopping'), style: const TextStyle(fontWeight: FontWeight.w700)),
            Text(o.errandDetails ?? ''),
          ] else
            OrderItemsList(o),
          const Divider(height: 18),
          Row(children: [
            Icon(cash ? Icons.payments_outlined : Icons.verified, color: cash ? cs.tertiary : cs.secondary),
            const SizedBox(width: 6),
            Expanded(
              child: Text(cash ? '${s.t('cashToCollect')}: ${s.money(o.total)}' : s.t('paidOnline'),
                  style: TextStyle(fontWeight: FontWeight.w800, color: cash ? cs.tertiary : cs.secondary)),
            ),
          ]),
          if (cash && o.cashChangeFor != null) Text('${s.t('changeFor')} ${s.money(o.cashChangeFor!)} → ${s.money(o.cashChangeFor! - o.total)}'),
          const SizedBox(height: 12),
          if (canPick)
            FilledButton.icon(
              onPressed: () => _step(o, 'pickup'),
              icon: const Icon(Icons.inventory_2_outlined),
              label: Text(o.isErrand ? s.t('boughtIt') : s.t('pickedUp')),
            ),
          if (o.status == 'PICKED_UP')
            FilledButton.icon(
              style: FilledButton.styleFrom(backgroundColor: cs.secondary),
              onPressed: () async {
                if (await confirmDialog(context, '${s.t('deliveredBtn')}؟')) _step(o, 'deliver');
              },
              icon: const Icon(Icons.done_all),
              label: Text(s.t('deliveredBtn')),
            ),
          if (o.status == 'ACCEPTED' && !o.isErrand) Text(s.t('stx_ACCEPTED'), style: TextStyle(color: cs.onSurfaceVariant)),
        ]),
      ),
    );
  }
}
