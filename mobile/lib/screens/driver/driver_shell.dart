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
    await busy(context, () => _s.api.post('/driver/status', {'online': v, 'vehicle': vehicle}));
    _load();
  }

  Future<void> _setVehicle(String v) async {
    setState(() => vehicle = v);
    await busy(context, () => _s.api.post('/driver/status', {'online': online, 'vehicle': v}));
    _load();
  }

  Future<void> _step(OrderView o, String action) async {
    await busy(context, () => _s.api.post('/driver/orders/${o.code}/$action'));
    _load();
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
                _hero(s, cs),
                const SizedBox(height: 14),
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
                    const SizedBox(width: 10),
                    Expanded(child: StatTile(Icons.route, '${history.where((o) => o.status == 'DELIVERED').length}', s.t('drvTrips'))),
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
                if (current.isEmpty && offers.isEmpty) ...[
                  const SizedBox(height: 8),
                  _howItWorks(s, cs),
                  const SizedBox(height: 10),
                  _tips(s, cs),
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

  /// Greeting + the big online switch.
  Widget _hero(AppState s, ColorScheme cs) {
    final name = (s.user?.name ?? '').split(' ').first;
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(20),
        gradient: LinearGradient(
          colors: online ? [cs.secondary, cs.secondary.withValues(alpha: 0.75)] : [cs.primary, cs.primary.withValues(alpha: 0.78)],
        ),
      ),
      padding: const EdgeInsets.fromLTRB(18, 16, 12, 16),
      child: Row(children: [
        Container(
          width: 52,
          height: 52,
          decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.18), shape: BoxShape.circle),
          child: Icon(_vehicleIcon(vehicle), color: Colors.white, size: 28),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(s.t('drvHello', {'n': name}), style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w900, fontSize: 19)),
            const SizedBox(height: 2),
            Text(online ? s.t('drvOnlineSub') : s.t('goOnline'), style: TextStyle(color: Colors.white.withValues(alpha: 0.9), fontSize: 13)),
          ]),
        ),
        Column(children: [
          // on: a white knob on a dark track (clearly "on" over the green card); off: a pale knob on a faint track
          Switch(value: online, onChanged: _setOnline, activeThumbColor: Colors.white,
              activeTrackColor: Colors.black.withValues(alpha: .28),
              inactiveThumbColor: Colors.white70,
              inactiveTrackColor: Colors.white24),
          Text(online ? s.t('online') : s.t('offline'), style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w800, fontSize: 12)),
        ]),
      ]),
    );
  }

  IconData _vehicleIcon(String v) => switch (v) {
        'BICYCLE' => Icons.pedal_bike,
        'CAR' => Icons.directions_car,
        'TUKTUK' => Icons.electric_rickshaw,
        _ => Icons.two_wheeler,
      };

  /// "2× Shawarma, 1× Pepsi" — what the driver will carry.
  String _itemsLine(AppState s, OrderView o) => o.isErrand
      ? (o.errandDetails ?? '')
      : o.items.map((i) => '${i.qty}× ${s.itemName(i)}').join('، ');

  Widget _infoRow(ColorScheme cs, IconData icon, String label, String value, {Color? color, bool bold = false}) => Padding(
        padding: const EdgeInsets.only(top: 8),
        child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Icon(icon, size: 20, color: color ?? cs.primary),
          const SizedBox(width: 8),
          Expanded(
            child: Text.rich(TextSpan(children: [
              TextSpan(text: '$label: ', style: TextStyle(color: cs.onSurfaceVariant, fontWeight: FontWeight.w700)),
              TextSpan(text: value, style: TextStyle(fontWeight: bold ? FontWeight.w900 : FontWeight.w600, color: color)),
            ])),
          ),
        ]),
      );

  Widget _offerCard(AppState s, ColorScheme cs, OrderView o) {
    final cash = o.paymentMethod == 'CASH';
    final address = [s.zoneName(o.zone), o.address].where((x) => x.trim().isNotEmpty).join(' — ');
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Card(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18), side: BorderSide(color: cs.primary.withValues(alpha: 0.35), width: 1.4)),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(color: cs.primaryContainer, borderRadius: BorderRadius.circular(20)),
                child: Text(s.t('drvOrder', {'c': o.code}), style: TextStyle(fontWeight: FontWeight.w900, color: cs.onPrimaryContainer)),
              ),
              const Spacer(),
              Column(crossAxisAlignment: CrossAxisAlignment.end, children: [
                Text(s.money(o.deliveryFee), style: TextStyle(color: cs.secondary, fontWeight: FontWeight.w900, fontSize: 20)),
                Text(s.t('drvDeliveryFee'), style: TextStyle(color: cs.onSurfaceVariant, fontSize: 11)),
              ]),
            ]),
            _infoRow(cs, o.isErrand ? Icons.shopping_basket : Icons.storefront, s.t('from'), _from(s, o), bold: true),
            _infoRow(cs, Icons.receipt_long, s.t('drvItems'), _itemsLine(s, o)),
            if ((o.customerName ?? '').isNotEmpty) _infoRow(cs, Icons.person, s.t('drvCustomer'), o.customerName!),
            _infoRow(cs, Icons.location_on, s.t('to'), address),
            if ((o.landmark ?? '').isNotEmpty) _infoRow(cs, Icons.flag, s.t('landmark'), o.landmark!, color: cs.secondary, bold: true),
            if (cash) _infoRow(cs, Icons.payments_outlined, s.t('cashToCollect'), s.money(o.total), color: cs.tertiary, bold: true),
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              child: FilledButton.icon(onPressed: () => _step(o, 'take'), icon: const Icon(Icons.check), label: Text(s.t('take'))),
            ),
          ]),
        ),
      ),
    );
  }

  Widget _howItWorks(AppState s, ColorScheme cs) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(s.t('drvHowTitle'), style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 16)),
          for (var i = 0; i < 4; i++)
            Padding(
              padding: const EdgeInsets.only(top: 10),
              child: Row(children: [
                CircleAvatar(
                  radius: 15,
                  backgroundColor: cs.primary,
                  child: Text('${i + 1}', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w900)),
                ),
                const SizedBox(width: 10),
                Expanded(child: Text(s.t('drvStep${i + 1}'))),
              ]),
            ),
        ]),
      ),
    );
  }

  Widget _tips(AppState s, ColorScheme cs) => Card(
        color: cs.tertiaryContainer.withValues(alpha: 0.5),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(children: [
              Icon(Icons.lightbulb_outline, color: cs.tertiary),
              const SizedBox(width: 6),
              Text(s.t('drvTips'), style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 16)),
            ]),
            for (final k in ['drvTip1', 'drvTip2', 'drvTip3'])
              Padding(padding: const EdgeInsets.only(top: 6), child: Text('•  ${s.t(k)}')),
          ]),
        ),
      );

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
          if ((o.customerName ?? '').isNotEmpty)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 4),
              child: Row(children: [
                Icon(Icons.person, color: cs.primary, size: 20),
                const SizedBox(width: 6),
                Text(o.customerName!, style: const TextStyle(fontWeight: FontWeight.w800)),
              ]),
            ),
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
          if (cash && o.cashChangeFor != null)
            Text(s.t('drvChange', {'a': s.money(o.cashChangeFor!), 'b': s.money(o.cashChangeFor! - o.total)}),
                style: TextStyle(color: cs.onSurfaceVariant, fontWeight: FontWeight.w700)),
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
