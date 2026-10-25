import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';
import 'package:provider/provider.dart';

import '../../core/app_state.dart';
import '../../core/models.dart';
import '../../widgets/common.dart';
import 'payment_screen.dart';

/// Live order tracking: status steps, the driver on the map, the order, and what the customer can do next
/// (pay, cancel, return, rate, report a problem, order again).
class TrackScreen extends StatefulWidget {
  final String code;
  const TrackScreen({super.key, required this.code});
  @override
  State<TrackScreen> createState() => _TrackScreenState();
}

class _TrackScreenState extends State<TrackScreen> {
  OrderView? order;
  Object? error;
  late final AppState _s;

  static const _steps = ['PLACED', 'ACCEPTED', 'READY', 'PICKED_UP', 'DELIVERED'];

  @override
  void initState() {
    super.initState();
    _s = context.read<AppState>();
    _load();
    _s.watchOrder(widget.code, _onOrder);
  }

  @override
  void dispose() {
    _s.unwatchOrder(widget.code, _onOrder);
    super.dispose();
  }

  void _onOrder(OrderView o) {
    if (mounted) setState(() => order = o);
  }

  Future<void> _load() async {
    try {
      final o = await _s.order(widget.code);
      if (mounted) setState(() { order = o; error = null; });
    } catch (e) {
      if (mounted) setState(() => error = e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    final o = order;
    if (o == null) {
      return Scaffold(
        appBar: AppBar(title: Text(widget.code)),
        body: error == null
            ? const Center(child: CircularProgressIndicator())
            : EmptyState(Icons.error_outline, s.errorText(error!), action: FilledButton(onPressed: _load, child: Text(s.t('retry')))),
      );
    }
    final failed = const {'REJECTED', 'CANCELLED', 'RETURNED'}.contains(o.status);
    final current = _steps.indexOf(o.status);
    final storeName = o.store == null ? s.t('errand') : (s.ar ? o.store!.nameAr : o.store!.name);
    final headline = o.isErrand && o.status == 'ACCEPTED' ? s.t('errandAccepted') : s.t('stx_${o.status}');
    return Scaffold(
      appBar: AppBar(title: Text('${s.t('orderCode')} ${o.code}')),
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(padding: const EdgeInsets.all(16), children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Row(children: [
                  Expanded(child: Text(storeName, style: tt.titleMedium?.copyWith(fontWeight: FontWeight.w900))),
                  StatusChip(o.status),
                ]),
                const SizedBox(height: 6),
                if (hasHeadline(s, o)) Text(headline, style: tt.titleLarge?.copyWith(fontWeight: FontWeight.w900, color: cs.primary)),
                if (o.status == 'REJECTED' && o.rejectReason != null) Text('${s.t('rejectReason')}: ${o.rejectReason}'),
                if (o.paymentStatus == 'REFUNDED') Text(s.t('refunded'), style: TextStyle(color: cs.secondary, fontWeight: FontWeight.w700)),
                if (o.scheduledFor != null) Text('${s.t('scheduledFor')}: ${timeText(context, o.scheduledFor)}'),
                if (!failed && !o.waitingPayment) ...[
                  const SizedBox(height: 16),
                  _Steps(steps: _steps, current: current, order: o),
                ],
                if (o.waitingPayment) ...[
                  const SizedBox(height: 12),
                  FilledButton.icon(
                    onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => PaymentScreen(code: o.code))),
                    icon: const Icon(Icons.payment),
                    label: Text(s.t('payNow')),
                  ),
                ],
              ]),
            ),
          ),
          if (o.active && (o.driver != null || o.lat != null || o.store?.lat != null)) ...[
            const SizedBox(height: 12),
            _TrackMap(order: o),
          ],
          if (o.driver != null && o.active) ...[
            const SizedBox(height: 12),
            Card(
              child: ListTile(
                leading: CircleAvatar(backgroundColor: cs.secondaryContainer, child: Icon(Icons.two_wheeler, color: cs.secondary)),
                title: Text(o.driver!.name, style: const TextStyle(fontWeight: FontWeight.w800)),
                subtitle: Text(s.t('driver')),
                trailing: o.driver!.phone == null
                    ? null
                    : IconButton.filled(onPressed: () => callPhone(o.driver!.phone!), icon: const Icon(Icons.call)),
              ),
            ),
          ],
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                AddressBlock(o),
                const Divider(height: 24),
                if (o.isErrand) Text('🛒 ${o.errandDetails ?? ''}') else OrderItemsList(o),
                const Divider(height: 24),
                if (!o.isErrand) PriceRow(s.t('subtotal'), s.money(o.subtotal)),
                PriceRow(s.t('deliveryFee'), o.deliveryFee == 0 ? s.t('freeDelivery') : s.money(o.deliveryFee)),
                if (o.discount > 0) PriceRow(s.t('discount'), '− ${s.money(o.discount)}', color: cs.secondary),
                PriceRow(s.t('total'), s.money(o.total), bold: true),
                PriceRow(s.t('payWith'), s.t('pay_${o.paymentMethod}')),
                if (o.cashChangeFor != null) PriceRow(s.t('changeFor'), s.money(o.cashChangeFor!)),
              ]),
            ),
          ),
          const SizedBox(height: 12),
          ..._actions(context, s, o),
          const SizedBox(height: 24),
        ]),
      ),
    );
  }

  bool hasHeadline(AppState s, OrderView o) => s.t('stx_${o.status}') != 'stx_${o.status}' || (o.isErrand && o.status == 'ACCEPTED');

  List<Widget> _actions(BuildContext context, AppState s, OrderView o) {
    final w = <Widget>[];
    if (o.status == 'PLACED' || o.waitingPayment) {
      w.add(OutlinedButton.icon(
        icon: const Icon(Icons.close),
        label: Text(s.t('cancelOrder')),
        onPressed: () async {
          if (!await confirmDialog(context, s.t('cancelSure'))) return;
          if (!context.mounted) return;
          final r = await busy(context, () => s.cancelOrder(o.code));
          if (r != null) setState(() => order = r);
        },
      ));
    }
    if (o.status == 'DELIVERED') {
      w.addAll([
        FilledButton.icon(icon: const Icon(Icons.star_outline), label: Text(s.t('rate')), onPressed: () => _rate(context, s, o)),
        const SizedBox(height: 8),
        if (o.store != null)
          OutlinedButton.icon(
            icon: const Icon(Icons.replay),
            label: Text(s.t('orderAgain')),
            onPressed: () async {
              final missing = await busy(context, () => s.reorder(o));
              if (missing != null && context.mounted) {
                context.read<AppState>().goTab(2);
                Navigator.of(context).popUntil((r) => r.isFirst);
              }
            },
          ),
        const SizedBox(height: 8),
        OutlinedButton.icon(
          icon: const Icon(Icons.undo),
          label: Text(s.t('returnOrder')),
          onPressed: () async {
            if (!await confirmDialog(context, s.t('returnSure'))) return;
            if (!context.mounted) return;
            final r = await busy(context, () => s.returnOrder(o.code));
            if (r != null) setState(() => order = r);
          },
        ),
      ]);
    }
    if (!o.waitingPayment) {
      w.addAll([
        const SizedBox(height: 8),
        TextButton.icon(icon: const Icon(Icons.support_agent), label: Text(s.t('complain')), onPressed: () => _complain(context, s, o)),
      ]);
    }
    return w;
  }

  Future<void> _rate(BuildContext context, AppState s, OrderView o) async {
    var store = 5, driver = 5;
    final comment = TextEditingController();
    final ok = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (c) => StatefulBuilder(
        builder: (c, set) => Padding(
          padding: EdgeInsets.fromLTRB(16, 0, 16, MediaQuery.of(c).viewInsets.bottom + 16),
          child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
            Text(s.t('rate'), style: Theme.of(c).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)),
            const SizedBox(height: 8),
            Text(o.isErrand ? s.t('errand') : s.t('rateStore')),
            StarPicker(store, (v) => set(() => store = v)),
            if (o.driver != null) ...[Text(s.t('rateDriver')), StarPicker(driver, (v) => set(() => driver = v))],
            TextField(controller: comment, maxLength: 1000, maxLines: 3, decoration: InputDecoration(labelText: s.t('comment'))),
            const SizedBox(height: 8),
            FilledButton(onPressed: () => Navigator.pop(c, true), child: Text(s.t('confirm'))),
          ]),
        ),
      ),
    );
    if (ok != true || !context.mounted) return;
    final done = await busy(context, () => s.review(o.code, store, o.driver == null ? null : driver, comment.text).then((_) => true));
    if (done == true && context.mounted) showInfo(context, s.t('thanksRating'));
  }

  Future<void> _complain(BuildContext context, AppState s, OrderView o) async {
    final details = TextEditingController();
    final ok = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: Text(s.t('complain')),
        content: TextField(controller: details, maxLines: 4, maxLength: 1000, decoration: InputDecoration(hintText: s.t('complaintHint'))),
        actions: [
          TextButton(onPressed: () => Navigator.pop(c, false), child: Text(s.t('cancel'))),
          FilledButton(onPressed: () => Navigator.pop(c, true), child: Text(s.t('confirm'))),
        ],
      ),
    );
    if (ok != true || !context.mounted) return;
    final code = await busy(context, () => s.complain(o.code, details.text));
    if (code != null && context.mounted) showInfo(context, '${s.t('complaintSent')} $code');
  }
}

class _Steps extends StatelessWidget {
  final List<String> steps;
  final int current;
  final OrderView order;
  const _Steps({required this.steps, required this.current, required this.order});

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    return Column(children: [
      for (var i = 0; i < steps.length; i++)
        Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Column(children: [
            Icon(i <= current ? Icons.check_circle : Icons.radio_button_unchecked, color: i <= current ? cs.secondary : cs.outline),
            if (i < steps.length - 1) Container(width: 2, height: 22, color: i < current ? cs.secondary : cs.outlineVariant),
          ]),
          const SizedBox(width: 10),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.only(top: 2),
              child: Text(s.t('st_${steps[i]}'),
                  style: TextStyle(fontWeight: i == current ? FontWeight.w900 : FontWeight.w500, color: i <= current ? null : cs.onSurfaceVariant)),
            ),
          ),
          Text(timeText(context, order.stepAt(steps[i])).split('·').last, style: TextStyle(color: cs.onSurfaceVariant, fontSize: 12.5)),
        ]),
    ]);
  }
}

/// OpenStreetMap with the store, the customer and (once on the way) the driver.
class _TrackMap extends StatelessWidget {
  final OrderView order;
  const _TrackMap({required this.order});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final points = <(LatLng, IconData, Color)>[
      if (order.store?.lat != null && order.store?.lng != null) (LatLng(order.store!.lat!, order.store!.lng!), Icons.storefront, cs.primary),
      if (order.lat != null && order.lng != null) (LatLng(order.lat!, order.lng!), Icons.home, cs.tertiary),
      if (order.driver?.lat != null && order.driver?.lng != null) (LatLng(order.driver!.lat!, order.driver!.lng!), Icons.two_wheeler, cs.secondary),
    ];
    // Samannoud's centre when nothing has coordinates yet
    final center = points.isEmpty ? const LatLng(30.9622, 31.2414) : points.last.$1;
    return ClipRRect(
      borderRadius: BorderRadius.circular(18),
      child: SizedBox(
        height: 220,
        child: FlutterMap(
          options: MapOptions(initialCenter: center, initialZoom: 15),
          children: [
            TileLayer(urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png', userAgentPackageName: 'com.velox.velox_app'),
            MarkerLayer(markers: [
              for (final p in points)
                Marker(
                  point: p.$1,
                  width: 44,
                  height: 44,
                  child: CircleAvatar(backgroundColor: p.$3, child: Icon(p.$2, color: Colors.white, size: 22)),
                ),
            ]),
          ],
        ),
      ),
    );
  }
}
