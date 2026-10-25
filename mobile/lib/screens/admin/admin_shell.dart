import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/app_state.dart';
import '../../core/models.dart';
import '../../widgets/common.dart';
import '../shared/settings_screen.dart';
import '../store/store_shell.dart' show StatTile;

/// The admin on the phone: today's numbers, every order (cancel with a reason) and complaints.
/// Stores, menus, areas, promo codes and users are managed from the web admin (bigger screen).
class AdminShell extends StatefulWidget {
  const AdminShell({super.key});
  @override
  State<AdminShell> createState() => _AdminShellState();
}

class _AdminShellState extends State<AdminShell> {
  int tab = 0;
  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    return Scaffold(
      body: IndexedStack(index: tab, children: const [_Dashboard(), _Orders(), _Complaints()]),
      bottomNavigationBar: NavigationBar(
        selectedIndex: tab,
        onDestinationSelected: (i) => setState(() => tab = i),
        destinations: [
          NavigationDestination(icon: const Icon(Icons.dashboard_outlined), label: s.t('dashboard')),
          NavigationDestination(icon: const Icon(Icons.receipt_long_outlined), label: s.t('allOrders')),
          NavigationDestination(icon: const Icon(Icons.support_agent), label: s.t('complaints')),
        ],
      ),
    );
  }
}

class _Dashboard extends StatefulWidget {
  const _Dashboard();
  @override
  State<_Dashboard> createState() => _DashboardState();
}

class _DashboardState extends State<_Dashboard> {
  Map<String, dynamic>? st;
  Object? error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final r = Map<String, dynamic>.from(await context.read<AppState>().api.get('/admin/stats'));
      if (mounted) setState(() { st = r; error = null; });
    } catch (e) {
      if (mounted) setState(() => error = e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final x = st;
    double n(String k) => (x?[k] as num?)?.toDouble() ?? 0;
    return Scaffold(
      appBar: AppBar(title: Text(s.t('dashboard')), actions: [
        IconButton(onPressed: _load, icon: const Icon(Icons.refresh)),
        IconButton(
            icon: const Icon(Icons.settings_outlined),
            onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const SettingsScreen()))),
      ]),
      body: error != null
          ? EmptyState(Icons.cloud_off, s.errorText(error!), action: FilledButton(onPressed: _load, child: Text(s.t('retry'))))
          : x == null
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
                        StatTile(Icons.receipt_long, '${x['ordersToday']}', s.t('ordersCount')),
                        StatTile(Icons.check_circle, '${x['deliveredToday']}', s.t('delivered')),
                        StatTile(Icons.payments, s.money(n('salesToday')), s.t('salesToday')),
                        StatTile(Icons.percent, s.money(n('commissionToday')), s.t('commission')),
                        StatTile(Icons.two_wheeler, '${x['driversOnline']}', s.t('driversOnline')),
                        StatTile(Icons.support_agent, '${x['openComplaints']}', s.t('openComplaints')),
                      ],
                    ),
                    const SizedBox(height: 16),
                    Text(s.t('adminWeb'), style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant)),
                  ]),
                ),
    );
  }
}

class _Orders extends StatefulWidget {
  const _Orders();
  @override
  State<_Orders> createState() => _OrdersState();
}

class _OrdersState extends State<_Orders> {
  List<OrderView> orders = [];
  String? status;
  Object? error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final r = await context.read<AppState>().api.get('/admin/orders${status == null ? '' : '?status=$status'}') as List;
      if (mounted) setState(() { orders = r.map((o) => OrderView.fromJson(o)).toList(); error = null; });
    } catch (e) {
      if (mounted) setState(() => error = e);
    }
  }

  Future<void> _cancel(OrderView o) async {
    final s = context.read<AppState>();
    final reason = TextEditingController();
    final ok = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: Text('${s.t('cancelOrder')} ${o.code}'),
        content: TextField(controller: reason, maxLength: 200, decoration: InputDecoration(labelText: s.t('rejectReason'))),
        actions: [
          TextButton(onPressed: () => Navigator.pop(c, false), child: Text(s.t('no'))),
          FilledButton(onPressed: () => Navigator.pop(c, reason.text.trim().isNotEmpty), child: Text(s.t('confirm'))),
        ],
      ),
    );
    if (ok == true && mounted) {
      await busy(context, () => s.api.post('/admin/orders/${o.code}/cancel', {'reason': reason.text.trim()}));
      _load();
    }
  }

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    const statuses = ['PLACED', 'ACCEPTED', 'READY', 'PICKED_UP', 'DELIVERED', 'CANCELLED', 'REJECTED'];
    return Scaffold(
      appBar: AppBar(title: Text(s.t('allOrders')), actions: [IconButton(onPressed: _load, icon: const Icon(Icons.refresh))]),
      body: Column(children: [
        SizedBox(
          height: 52,
          child: ListView(padding: const EdgeInsets.fromLTRB(12, 8, 12, 0), scrollDirection: Axis.horizontal, children: [
            Padding(
              padding: const EdgeInsetsDirectional.only(end: 6),
              child: ChoiceChip(label: Text(s.t('allTypes')), selected: status == null, onSelected: (_) => {status = null, _load()}),
            ),
            for (final st in statuses)
              Padding(
                padding: const EdgeInsetsDirectional.only(end: 6),
                child: ChoiceChip(label: Text(s.t('st_$st')), selected: status == st, onSelected: (_) => {status = st, _load()}),
              ),
          ]),
        ),
        Expanded(
          child: error != null
              ? EmptyState(Icons.cloud_off, s.errorText(error!))
              : orders.isEmpty
                  ? EmptyState(Icons.inbox_outlined, s.t('empty'))
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView.separated(
                        itemCount: orders.length,
                        separatorBuilder: (_, _) => const Divider(),
                        itemBuilder: (c, i) {
                          final o = orders[i];
                          final name = o.store == null ? s.t('errand') : (s.ar ? o.store!.nameAr : o.store!.name);
                          return ListTile(
                            title: Text('${o.code} · $name', style: const TextStyle(fontWeight: FontWeight.w800)),
                            subtitle: Text('${o.customerName ?? ''} · ${s.zoneName(o.zone)} · ${s.money(o.total)}\n${timeText(context, o.createdAt)}'),
                            isThreeLine: true,
                            trailing: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
                              StatusChip(o.status),
                              if (o.active || o.waitingPayment)
                                InkWell(onTap: () => _cancel(o), child: Text(s.t('cancel'), style: TextStyle(color: Theme.of(context).colorScheme.error))),
                            ]),
                          );
                        },
                      ),
                    ),
        ),
      ]),
    );
  }
}

class _Complaints extends StatefulWidget {
  const _Complaints();
  @override
  State<_Complaints> createState() => _ComplaintsState();
}

class _ComplaintsState extends State<_Complaints> {
  List<Complaint> list = [];
  Object? error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final r = await context.read<AppState>().api.get('/admin/complaints') as List;
      if (mounted) setState(() { list = r.map((c) => Complaint.fromJson(c)).toList(); error = null; });
    } catch (e) {
      if (mounted) setState(() => error = e);
    }
  }

  Future<void> _close(Complaint c) async {
    final s = context.read<AppState>();
    final note = TextEditingController(), comp = TextEditingController();
    final ok = await showDialog<bool>(
      context: context,
      builder: (d) => AlertDialog(
        title: Text('${s.t('closeComplaint')} ${c.code}'),
        content: Column(mainAxisSize: MainAxisSize.min, children: [
          Text(c.details),
          const SizedBox(height: 10),
          TextField(controller: note, maxLength: 500, decoration: InputDecoration(labelText: s.t('comment'))),
          TextField(controller: comp, keyboardType: TextInputType.number, decoration: InputDecoration(labelText: s.t('compensation'))),
        ]),
        actions: [
          TextButton(onPressed: () => Navigator.pop(d, false), child: Text(s.t('cancel'))),
          FilledButton(onPressed: () => Navigator.pop(d, true), child: Text(s.t('confirm'))),
        ],
      ),
    );
    if (ok == true && mounted) {
      await busy(context, () => s.api.post('/admin/complaints/${c.code}/close', {
            if (note.text.trim().isNotEmpty) 'note': note.text.trim(),
            if (double.tryParse(comp.text.trim()) != null) 'compensation': double.parse(comp.text.trim()),
          }));
      _load();
    }
  }

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: Text(s.t('complaints')), actions: [IconButton(onPressed: _load, icon: const Icon(Icons.refresh))]),
      body: error != null
          ? EmptyState(Icons.cloud_off, s.errorText(error!))
          : list.isEmpty
              ? EmptyState(Icons.sentiment_satisfied_alt, s.t('empty'))
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView(children: [
                    for (final c in list)
                      ListTile(
                        leading: Icon(c.status == 'OPEN' ? Icons.mark_email_unread : Icons.check_circle, color: c.status == 'OPEN' ? cs.tertiary : cs.secondary),
                        title: Text('${c.code} · ${c.orderCode}', style: const TextStyle(fontWeight: FontWeight.w800)),
                        subtitle: Text('${c.details}${c.compensationCode == null ? '' : '\n🎁 ${c.compensationCode}'}'),
                        isThreeLine: true,
                        onTap: c.status == 'OPEN' ? () => _close(c) : null,
                      ),
                  ]),
                ),
    );
  }
}
