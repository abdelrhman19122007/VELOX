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
  Store? store;
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
      Store? st;
      try {
        st = StoreDetail.fromJson(await _s.api.get('/restaurant/store')).store;
      } catch (_) {}
      if (!mounted) return;
      setState(() {
        if (st != null) store = st;
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
    // The server may answer with an empty body, so always refresh instead of checking the result.
    await busy(context, () => _s.api.post('/restaurant/orders/$code/$action', reason == null ? null : {'reason': reason}));
    await _load();
  }

  Future<void> _setAccepting(bool v) async {
    await busy(context, () => _s.api.patch('/restaurant/store', {'accepting': v}));
    _load();
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
    final cs = Theme.of(context).colorScheme;
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
        ),
        body: loading
            ? const Center(child: CircularProgressIndicator())
            : error != null
                ? EmptyState(Icons.cloud_off, s.errorText(error!), action: FilledButton(onPressed: _load, child: Text(s.t('retry'))))
                : Column(children: [
                    Padding(
                      padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
                      child: _hero(s, cs, newOnes.length, preparing.length, ready.length),
                    ),
                    TabBar(isScrollable: true, tabAlignment: TabAlignment.start, tabs: [
                      Tab(
                        child: Text('${s.t('tabNew')} (${newOnes.length})',
                            style: newOnes.isEmpty ? null : TextStyle(color: cs.error, fontWeight: FontWeight.w900)),
                      ),
                      Tab(text: '${s.t('tabPreparing')} (${preparing.length})'),
                      Tab(text: '${s.t('tabReady')} (${ready.length})'),
                      Tab(text: s.t('tabDone')),
                    ]),
                    Expanded(
                      child: TabBarView(children: [
                        _list(newOnes, (o) => [
                              FilledButton.icon(onPressed: () => _act(o.code, 'accept'), icon: const Icon(Icons.check), label: Text(s.t('accept'))),
                              OutlinedButton(onPressed: () => _reject(o), child: Text(s.t('reject'))),
                            ], guide: true),
                        _list(preparing, (o) => [
                              FilledButton.icon(
                                style: FilledButton.styleFrom(backgroundColor: cs.secondary),
                                onPressed: () => _act(o.code, 'ready'),
                                icon: const Icon(Icons.room_service),
                                label: Text(s.t('markReady')),
                              ),
                            ]),
                        _list(ready, (o) => []),
                        _list(done, (o) => [], compact: true),
                      ]),
                    ),
                  ]),
      ),
    );
  }

  /// Store name + the "accepting orders" switch + live counters.
  Widget _hero(AppState s, ColorScheme cs, int waiting, int kitchen, int withDriver) {
    final open = store?.accepting ?? true;
    final name = store == null ? (s.user?.name ?? '') : s.storeName(store!);
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(20),
        gradient: LinearGradient(
          colors: open ? [cs.primary, cs.primary.withValues(alpha: 0.78)] : [Colors.blueGrey, Colors.blueGrey.withValues(alpha: 0.75)],
        ),
      ),
      padding: const EdgeInsets.fromLTRB(14, 10, 10, 10),
      child: Column(children: [
        Row(children: [
          Container(
            width: 48,
            height: 48,
            decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.18), shape: BoxShape.circle),
            child: const Icon(Icons.storefront, color: Colors.white, size: 26),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(s.t('stHello', {'n': name}),
                  maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w900, fontSize: 18)),
              const SizedBox(height: 2),
              Text(open ? s.t('stOpenSub') : s.t('stClosedSub'), style: TextStyle(color: Colors.white.withValues(alpha: 0.9), fontSize: 13)),
            ]),
          ),
          if (store != null)
            Switch(
              value: open,
              onChanged: _setAccepting,
              activeThumbColor: Colors.white,
              activeTrackColor: cs.secondary,
              inactiveThumbColor: Colors.white,
              inactiveTrackColor: Colors.white24,
            ),
        ]),
        const SizedBox(height: 8),
        Row(children: [
          _counter(s.t('stWaiting'), waiting),
          _counter(s.t('stInKitchen'), kitchen),
          _counter(s.t('stForDriver'), withDriver),
        ]),
      ]),
    );
  }

  Widget _counter(String label, int n) => Expanded(
        child: Container(
          margin: const EdgeInsets.symmetric(horizontal: 3),
          padding: const EdgeInsets.symmetric(vertical: 5),
          decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.16), borderRadius: BorderRadius.circular(12)),
          child: Column(children: [
            Text('$n', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w900, fontSize: 18)),
            Text(label, style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w600)),
          ]),
        ),
      );

  /// Shown under an empty "New" tab: how an order flows + tips.
  Widget _guide(AppState s, ColorScheme cs) => Column(children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(s.t('stHowTitle'), style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 16)),
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
                    Expanded(child: Text(s.t('stStep${i + 1}'))),
                  ]),
                ),
            ]),
          ),
        ),
        const SizedBox(height: 10),
        Card(
          color: cs.tertiaryContainer.withValues(alpha: 0.5),
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Row(children: [
                Icon(Icons.lightbulb_outline, color: cs.tertiary),
                const SizedBox(width: 6),
                Text(s.t('stTips'), style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 16)),
              ]),
              for (final k in ['stTip1', 'stTip2', 'stTip3'])
                Padding(padding: const EdgeInsets.only(top: 6), child: Text('•  ${s.t(k)}')),
            ]),
          ),
        ),
      ]);

  Widget _list(List<OrderView> list, List<Widget> Function(OrderView) actions, {bool guide = false, bool compact = false}) {
    final s = context.read<AppState>();
    final cs = Theme.of(context).colorScheme;
    if (list.isEmpty) {
      return RefreshIndicator(
        onRefresh: _load,
        child: ListView(padding: const EdgeInsets.all(16), children: [
          EmptyState(Icons.inbox_outlined, s.t('empty')),
          if (guide) _guide(s, cs),
        ]),
      );
    }
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView.separated(
        padding: const EdgeInsets.all(16),
        itemCount: list.length,
        separatorBuilder: (_, _) => const SizedBox(height: 12),
        itemBuilder: (c, i) => _StoreOrderCard(list[i], actions(list[i]), compact: compact),
      ),
    );
  }
}

class _StoreOrderCard extends StatelessWidget {
  final OrderView order;
  final List<Widget> actions;
  final bool compact;
  const _StoreOrderCard(this.order, this.actions, {this.compact = false});

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    final cash = order.paymentMethod == 'CASH';
    final waiting = order.status == 'PLACED' || order.status == 'ACCEPTED';
    final mins = order.createdAt == null ? null : DateTime.now().difference(order.createdAt!.toLocal()).inMinutes;
    final late = waiting && mins != null && mins >= 15;
    final count = order.items.fold<int>(0, (a, i) => a + i.qty);
    final withDriverStage = order.status == 'READY' || order.status == 'PICKED_UP';
    return Card(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(18),
        side: order.status == 'PLACED' ? BorderSide(color: cs.primary.withValues(alpha: 0.45), width: 1.5) : BorderSide.none,
      ),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
              decoration: BoxDecoration(color: cs.primaryContainer, borderRadius: BorderRadius.circular(20)),
              child: Text(order.code, style: TextStyle(fontWeight: FontWeight.w900, color: cs.onPrimaryContainer)),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: waiting && mins != null
                  ? Row(children: [
                      Icon(Icons.schedule, size: 16, color: late ? cs.error : cs.onSurfaceVariant),
                      const SizedBox(width: 3),
                      Flexible(
                        child: Text(
                          late ? '${s.t('stAgo', {'m': mins})} · ${s.t('stLate')}' : s.t('stAgo', {'m': mins}),
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                              color: late ? cs.error : cs.onSurfaceVariant, fontWeight: late ? FontWeight.w800 : FontWeight.w500, fontSize: 13),
                        ),
                      ),
                    ])
                  : Text(timeText(context, order.createdAt), overflow: TextOverflow.ellipsis, style: TextStyle(color: cs.onSurfaceVariant, fontSize: 13)),
            ),
            StatusChip(order.status),
          ]),
          if (order.scheduledFor != null)
            Padding(
              padding: const EdgeInsets.only(top: 6),
              child: Text('⏰ ${s.t('scheduledFor')}: ${timeText(context, order.scheduledFor)}',
                  style: TextStyle(color: cs.tertiary, fontWeight: FontWeight.w700)),
            ),
          if ((order.customerName ?? '').isNotEmpty)
            Padding(
              padding: const EdgeInsets.only(top: 8),
              child: Row(children: [
                Icon(Icons.person, size: 18, color: cs.primary),
                const SizedBox(width: 6),
                Text('${s.t('drvCustomer')}: ', style: TextStyle(color: cs.onSurfaceVariant, fontWeight: FontWeight.w700)),
                Expanded(child: Text(order.customerName!, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w700))),
                Text(s.t('stItemsCount', {'n': count}), style: TextStyle(color: cs.onSurfaceVariant, fontSize: 13)),
              ]),
            ),
          if (!compact) ...[
            const SizedBox(height: 8),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
              decoration: BoxDecoration(color: cs.surfaceContainerHighest.withValues(alpha: 0.6), borderRadius: BorderRadius.circular(12)),
              child: OrderItemsList(order),
            ),
          ],
          const Divider(height: 18),
          Row(children: [
            Icon(cash ? Icons.payments_outlined : Icons.verified, color: cash ? cs.tertiary : cs.secondary, size: 20),
            const SizedBox(width: 6),
            Text(cash ? s.t('cashToCollect') : s.t('paidOnline'),
                style: TextStyle(fontWeight: FontWeight.w700, color: cash ? cs.tertiary : cs.secondary)),
            const Spacer(),
            Text(s.money(order.total), style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 17)),
          ]),
          if (!compact && withDriverStage)
            Padding(
              padding: const EdgeInsets.only(top: 10),
              child: Container(
                padding: const EdgeInsets.fromLTRB(10, 6, 6, 6),
                decoration: BoxDecoration(
                  color: (order.driver != null ? cs.secondary : cs.tertiary).withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Row(children: [
                  Icon(order.driver != null ? Icons.delivery_dining : Icons.hourglass_top,
                      color: order.driver != null ? cs.secondary : cs.tertiary),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      order.driver != null ? s.t('stDriverOnWay', {'n': order.driver!.name}) : s.t('stNoDriverYet'),
                      style: const TextStyle(fontWeight: FontWeight.w700),
                    ),
                  ),
                  if ((order.driver?.phone ?? '').isNotEmpty)
                    IconButton.filledTonal(onPressed: () => callPhone(order.driver!.phone!), icon: const Icon(Icons.call)),
                ]),
              ),
            )
          else if (order.driver != null)
            Padding(padding: const EdgeInsets.only(top: 6), child: Text('🛵 ${order.driver!.name}')),
          if (order.status == 'REJECTED' && (order.rejectReason ?? '').isNotEmpty)
            Padding(padding: const EdgeInsets.only(top: 6), child: Text(order.rejectReason!, style: TextStyle(color: cs.error))),
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
    final sales = st == null ? 0.0 : (st['salesToday'] as num).toDouble();
    final delivered = st == null ? 0 : (st['deliveredToday'] as num).toInt();
    final avgRating = reviews.isEmpty ? null : reviews.map((r) => r.storeRating).reduce((a, b) => a + b) / reviews.length;
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
                        StatTile(Icons.check_circle, '$delivered', s.t('delivered')),
                        StatTile(Icons.cancel, '${st['cancelledToday']}', s.t('cancelled')),
                        StatTile(Icons.payments, s.money(sales), s.t('sales')),
                        StatTile(Icons.shopping_bag_outlined, delivered == 0 ? '—' : s.money(sales / delivered), s.t('stAvgOrder')),
                        StatTile(Icons.star_rounded, avgRating == null ? '—' : '${avgRating.toStringAsFixed(1)} ★', s.t('stRating')),
                      ],
                    ),
                    SectionTitle(s.t('reviews')),
                    if (reviews.isEmpty) EmptyState(Icons.star_outline, s.t('empty')),
                    for (final r in reviews)
                      Card(
                        child: ListTile(
                          leading: Text('${'★' * r.storeRating}${'☆' * (5 - r.storeRating)}', style: const TextStyle(color: Color(0xFFE8A317))),
                          title: Text(r.comment ?? r.orderCode),
                          subtitle: Text('${r.orderCode} · ${timeText(context, r.at)}'),
                        ),
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
