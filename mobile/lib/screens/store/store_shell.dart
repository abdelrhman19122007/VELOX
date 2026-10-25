import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../../core/app_state.dart';
import '../../core/models.dart';
import '../../widgets/common.dart';
import '../customer/store_screen.dart' show sectionName;
import '../shared/settings_screen.dart';

/// The store's app (restaurant, shop, pharmacy staff): live orders board, today's numbers, menu and reviews.
class StoreShell extends StatefulWidget {
  const StoreShell({super.key});
  @override
  State<StoreShell> createState() => _StoreShellState();
}

class _StoreShellState extends State<StoreShell> {
  int tab = 0;
  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    return Scaffold(
      body: IndexedStack(index: tab, children: const [_OrdersBoard(), _MenuTab(), _StatsTab()]),
      bottomNavigationBar: NavigationBar(
        selectedIndex: tab,
        onDestinationSelected: (i) => setState(() => tab = i),
        destinations: [
          NavigationDestination(icon: const Icon(Icons.receipt_long_outlined), label: s.t('storeOrders')),
          NavigationDestination(icon: const Icon(Icons.restaurant_menu), label: s.t('menu')),
          NavigationDestination(icon: const Icon(Icons.insights_outlined), label: s.t('today')),
        ],
      ),
    );
  }
}

class _OrdersBoard extends StatefulWidget {
  const _OrdersBoard();
  @override
  State<_OrdersBoard> createState() => _OrdersBoardState();
}

class _OrdersBoardState extends State<_OrdersBoard> {
  List<OrderView> active = [], done = [];
  bool loading = true;
  Object? error;
  late final AppState _s;
  late final void Function(String, dynamic) _listener;

  @override
  void initState() {
    super.initState();
    _s = context.read<AppState>();
    _listener = (event, data) {
      if (event != 'order' || data is! Map) return;
      final o = OrderView.fromJson(Map<String, dynamic>.from(data));
      final isNew = o.status == 'PLACED' && !active.any((x) => x.code == o.code);
      if (isNew) {
        SystemSound.play(SystemSoundType.alert);
        HapticFeedback.heavyImpact();
        if (mounted) showInfo(context, '🔔 ${_s.t('newOrderAlert')} ${o.code}');
      }
      _load();
    };
    _s.extraListeners.add(_listener);
    _load();
  }

  @override
  void dispose() {
    _s.extraListeners.remove(_listener);
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final a = await _s.api.get('/restaurant/orders?active=true') as List;
      final d = await _s.api.get('/restaurant/orders?active=false') as List;
      if (!mounted) return;
      setState(() {
        active = a.map((o) => OrderView.fromJson(o)).toList();
        done = d.map((o) => OrderView.fromJson(o)).toList();
        loading = false;
        error = null;
      });
    } catch (e) {
      if (mounted) setState(() { error = e; loading = false; });
    }
  }

  Future<void> _act(String code, String action, [String? reason]) async {
    final r = await busy(context, () => _s.api.post('/restaurant/orders/$code/$action', reason == null ? null : {'reason': reason}));
    if (r != null) _load();
  }

  Future<void> _reject(OrderView o) async {
    final s = _s;
    final other = TextEditingController();
    final reason = await showModalBottomSheet<String>(
      context: context,
      isScrollControlled: true,
      builder: (c) => Padding(
        padding: EdgeInsets.fromLTRB(16, 0, 16, MediaQuery.of(c).viewInsets.bottom + 16),
        child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          Text(s.t('rejectTitle'), style: Theme.of(c).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)),
          for (final k in ['reason_OUT', 'reason_CLOSING', 'reason_BUSY'])
            ListTile(title: Text(s.t(k)), leading: const Icon(Icons.chevron_left), onTap: () => Navigator.pop(c, s.t(k))),
          TextField(controller: other, maxLength: 200, decoration: InputDecoration(labelText: s.t('otherReason'))),
          FilledButton(
            style: FilledButton.styleFrom(backgroundColor: Theme.of(c).colorScheme.error),
            onPressed: () => other.text.trim().isEmpty ? null : Navigator.pop(c, other.text.trim()),
            child: Text(s.t('reject')),
          ),
        ]),
      ),
    );
    if (reason != null) _act(o.code, 'reject', reason);
  }

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final newOnes = active.where((o) => o.status == 'PLACED').toList();
    final preparing = active.where((o) => o.status == 'ACCEPTED').toList();
    final ready = active.where((o) => o.status == 'READY' || o.status == 'PICKED_UP').toList();
    return DefaultTabController(
      length: 4,
      child: Scaffold(
        appBar: AppBar(
          title: Text(s.t('storeOrders')),
          actions: [
            IconButton(icon: const Icon(Icons.refresh), onPressed: _load),
            IconButton(
                icon: const Icon(Icons.settings_outlined),
                onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const SettingsScreen()))),
          ],
          bottom: TabBar(isScrollable: true, tabs: [
            Tab(child: Badge(isLabelVisible: newOnes.isNotEmpty, label: Text('${newOnes.length}'), child: Text(s.t('tabNew')))),
            Tab(text: '${s.t('tabPreparing')} (${preparing.length})'),
            Tab(text: '${s.t('tabReady')} (${ready.length})'),
            Tab(text: s.t('tabDone')),
          ]),
        ),
        body: loading
            ? const Center(child: CircularProgressIndicator())
            : error != null
                ? EmptyState(Icons.cloud_off, s.errorText(error!), action: FilledButton(onPressed: _load, child: Text(s.t('retry'))))
                : TabBarView(children: [
                    _list(newOnes, (o) => [
                          FilledButton(onPressed: () => _act(o.code, 'accept'), child: Text(s.t('accept'))),
                          OutlinedButton(onPressed: () => _reject(o), child: Text(s.t('reject'))),
                        ]),
                    _list(preparing, (o) => [FilledButton(onPressed: () => _act(o.code, 'ready'), child: Text(s.t('markReady')))]),
                    _list(ready, (o) => []),
                    _list(done, (o) => []),
                  ]),
      ),
    );
  }

  Widget _list(List<OrderView> list, List<Widget> Function(OrderView) actions) {
    final s = context.read<AppState>();
    if (list.isEmpty) return RefreshIndicator(onRefresh: _load, child: ListView(children: [const SizedBox(height: 60), EmptyState(Icons.inbox_outlined, s.t('empty'))]));
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView.separated(
        padding: const EdgeInsets.all(16),
        itemCount: list.length,
        separatorBuilder: (_, _) => const SizedBox(height: 12),
        itemBuilder: (c, i) => _StoreOrderCard(list[i], actions(list[i])),
      ),
    );
  }
}

class _StoreOrderCard extends StatelessWidget {
  final OrderView order;
  final List<Widget> actions;
  const _StoreOrderCard(this.order, this.actions);
  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    final cash = order.paymentMethod == 'CASH';
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            Text(order.code, style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 17)),
            const SizedBox(width: 8),
            Text(timeText(context, order.createdAt).split('·').last, style: TextStyle(color: cs.onSurfaceVariant)),
            const Spacer(),
            StatusChip(order.status),
          ]),
          if (order.scheduledFor != null)
            Text('⏰ ${s.t('scheduledFor')}: ${timeText(context, order.scheduledFor)}', style: TextStyle(color: cs.tertiary, fontWeight: FontWeight.w700)),
          const Divider(height: 18),
          OrderItemsList(order),
          const Divider(height: 18),
          Row(children: [
            Icon(cash ? Icons.payments_outlined : Icons.verified, color: cash ? cs.tertiary : cs.secondary, size: 20),
            const SizedBox(width: 6),
            Text(cash ? s.t('cashToCollect') : s.t('paidOnline'), style: TextStyle(fontWeight: FontWeight.w700, color: cash ? cs.tertiary : cs.secondary)),
            const Spacer(),
            Text(s.money(order.total), style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 16)),
          ]),
          if (order.driver != null) Text('🛵 ${order.driver!.name}'),
          if (actions.isNotEmpty) ...[
            const SizedBox(height: 10),
            Row(children: [for (final a in actions) Expanded(child: Padding(padding: const EdgeInsets.symmetric(horizontal: 4), child: a))]),
          ],
        ]),
      ),
    );
  }
}

class _MenuTab extends StatefulWidget {
  const _MenuTab();
  @override
  State<_MenuTab> createState() => _MenuTabState();
}

class _MenuTabState extends State<_MenuTab> {
  StoreDetail? detail;
  Object? error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final d = StoreDetail.fromJson(await context.read<AppState>().api.get('/restaurant/store'));
      if (mounted) setState(() { detail = d; error = null; });
    } catch (e) {
      if (mounted) setState(() => error = e);
    }
  }

  Future<void> _price(Product p) async {
    final s = context.read<AppState>();
    final c = TextEditingController(text: p.price.toStringAsFixed(p.price % 1 == 0 ? 0 : 2));
    final ok = await showDialog<bool>(
      context: context,
      builder: (d) => AlertDialog(
        title: Text(s.productName(p)),
        content: TextField(controller: c, keyboardType: TextInputType.number, decoration: InputDecoration(labelText: s.t('price'))),
        actions: [
          TextButton(onPressed: () => Navigator.pop(d, false), child: Text(s.t('cancel'))),
          FilledButton(onPressed: () => Navigator.pop(d, true), child: Text(s.t('save'))),
        ],
      ),
    );
    final v = double.tryParse(c.text.trim());
    if (ok == true && v != null && mounted) {
      await busy(context, () => s.api.patch('/restaurant/products/${p.id}', {'price': v}));
      _load();
    }
  }

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final d = detail;
    return Scaffold(
      appBar: AppBar(title: Text(d == null ? s.t('menu') : s.storeName(d.store))),
      body: error != null
          ? EmptyState(Icons.cloud_off, s.errorText(error!), action: FilledButton(onPressed: _load, child: Text(s.t('retry'))))
          : d == null
              ? const Center(child: CircularProgressIndicator())
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView(children: [
                    SwitchListTile(
                      secondary: const Icon(Icons.storefront),
                      title: Text(s.t('acceptingOrders'), style: const TextStyle(fontWeight: FontWeight.w800)),
                      value: d.store.accepting,
                      onChanged: (v) async {
                        await busy(context, () => s.api.patch('/restaurant/store', {'accepting': v}));
                        _load();
                      },
                    ),
                    for (final sec in d.sections) ...[
                      SectionTitle(sectionName(s, sec.code)),
                      for (final p in sec.products)
                        ListTile(
                          leading: ProductImage(p.photoUrl, size: 48),
                          title: Text(s.productName(p)),
                          subtitle: InkWell(onTap: () => _price(p), child: Text('${s.money(p.price)}  ✎')),
                          trailing: Switch(
                            value: p.available,
                            onChanged: (v) async {
                              await busy(context, () => s.api.patch('/restaurant/products/${p.id}', {'available': v}));
                              _load();
                            },
                          ),
                        ),
                    ],
                  ]),
                ),
    );
  }
}

class _StatsTab extends StatefulWidget {
  const _StatsTab();
  @override
  State<_StatsTab> createState() => _StatsTabState();
}

class _StatsTabState extends State<_StatsTab> {
  Map<String, dynamic>? stats;
  List<Review> reviews = [];
  Object? error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final s = context.read<AppState>();
    try {
      final st = Map<String, dynamic>.from(await s.api.get('/restaurant/stats'));
      final rv = (await s.api.get('/restaurant/reviews') as List).map((r) => Review.fromJson(r)).toList();
      if (mounted) setState(() { stats = st; reviews = rv; error = null; });
    } catch (e) {
      if (mounted) setState(() => error = e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final st = stats;
    return Scaffold(
      appBar: AppBar(title: Text(s.t('today')), actions: [IconButton(onPressed: _load, icon: const Icon(Icons.refresh))]),
      body: error != null
          ? EmptyState(Icons.cloud_off, s.errorText(error!))
          : st == null
              ? const Center(child: CircularProgressIndicator())
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView(padding: const EdgeInsets.all(16), children: [
                    GridView.count(
                      crossAxisCount: 2,
                      shrinkWrap: true,
                      physics: const NeverScrollableScrollPhysics(),
                      mainAxisSpacing: 10,
                      crossAxisSpacing: 10,
                      childAspectRatio: 1.6,
                      children: [
                        StatTile(Icons.receipt_long, '${st['ordersToday']}', s.t('ordersCount')),
                        StatTile(Icons.check_circle, '${st['deliveredToday']}', s.t('delivered')),
                        StatTile(Icons.cancel, '${st['cancelledToday']}', s.t('cancelled')),
                        StatTile(Icons.payments, s.money((st['salesToday'] as num).toDouble()), s.t('sales')),
                      ],
                    ),
                    SectionTitle(s.t('reviews')),
                    if (reviews.isEmpty) EmptyState(Icons.star_outline, s.t('empty')),
                    for (final r in reviews)
                      ListTile(
                        leading: Text('${'★' * r.storeRating}${'☆' * (5 - r.storeRating)}', style: const TextStyle(color: Color(0xFFE8A317))),
                        title: Text(r.comment ?? r.orderCode),
                        subtitle: Text('${r.orderCode} · ${timeText(context, r.at)}'),
                      ),
                  ]),
                ),
    );
  }
}

/// A number with a label, used on dashboards.
class StatTile extends StatelessWidget {
  final IconData icon;
  final String value, label;
  const StatTile(this.icon, this.value, this.label, {super.key});
  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisAlignment: MainAxisAlignment.center, children: [
          Icon(icon, color: cs.primary),
          const SizedBox(height: 4),
          FittedBox(child: Text(value, style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w900))),
          Text(label, style: TextStyle(color: cs.onSurfaceVariant)),
        ]),
      ),
    );
  }
}
