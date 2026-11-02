import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/app_state.dart';
import '../../core/models.dart';
import '../../widgets/common.dart';
import 'track_screen.dart';

/// My orders: the ones still going, then the past ones (with "order again").
class OrdersScreen extends StatelessWidget {
  const OrdersScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final active = s.myOrders.where((o) => o.active || o.waitingPayment).toList();
    final past = s.myOrders.where((o) => !(o.active || o.waitingPayment)).toList();
    return Scaffold(
      appBar: AppBar(title: Text(s.t('orders'))),
      body: RefreshIndicator(
        onRefresh: s.refreshOrders,
        child: s.myOrders.isEmpty
            ? ListView(padding: const EdgeInsets.fromLTRB(24, 40, 24, 24), children: [
                EmptyState(Icons.receipt_long_outlined, s.t('ordersEmptyTitle'),
                    subtitle: s.t('ordersEmptySub'),
                    action: FilledButton.icon(
                        icon: const Icon(Icons.restaurant_menu),
                        onPressed: () => context.read<AppState>().goTab(0),
                        label: Text(s.t('browse')))),
                const SizedBox(height: 8),
                for (final (icon, key) in [
                  (Icons.location_on_outlined, 'tipTrack'),
                  (Icons.payments_outlined, 'tipCash'),
                  (Icons.replay, 'tipAgain'),
                ])
                  Card(
                    margin: const EdgeInsets.only(bottom: 10),
                    child: ListTile(
                      leading: Icon(icon, color: Theme.of(context).colorScheme.secondary),
                      title: Text(s.t(key)),
                    ),
                  ),
              ])
            : ListView(padding: const EdgeInsets.only(bottom: 24), children: [
                if (active.isNotEmpty) SectionTitle(s.t('active')),
                for (final o in active) _OrderCard(o),
                if (past.isNotEmpty) SectionTitle(s.t('history')),
                for (final o in past) _OrderCard(o),
              ]),
      ),
    );
  }
}

class _OrderCard extends StatelessWidget {
  final OrderView order;
  const _OrderCard(this.order);

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    final name = order.store == null ? s.t('errand') : (s.ar ? order.store!.nameAr : order.store!.name);
    final summary = order.isErrand
        ? (order.errandDetails ?? '')
        : order.items.map((i) => '${i.qty}× ${s.itemName(i)}').join('، ');
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
      child: Card(
        child: InkWell(
          onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => TrackScreen(code: order.code))),
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Row(children: [
                Icon(order.isErrand ? Icons.shopping_basket_outlined : Icons.storefront_outlined, color: cs.primary),
                const SizedBox(width: 8),
                Expanded(child: Text(name, style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 16))),
                StatusChip(order.status),
              ]),
              const SizedBox(height: 6),
              Text(summary, maxLines: 2, overflow: TextOverflow.ellipsis, style: TextStyle(color: cs.onSurfaceVariant)),
              const SizedBox(height: 6),
              Row(children: [
                Text('${order.code} · ${timeText(context, order.createdAt)}', style: TextStyle(color: cs.onSurfaceVariant, fontSize: 12.5)),
                const Spacer(),
                Text(s.money(order.total), style: const TextStyle(fontWeight: FontWeight.w900)),
              ]),
              if (order.status == 'DELIVERED' && order.store != null)
                Align(
                  alignment: AlignmentDirectional.centerEnd,
                  child: TextButton.icon(
                    icon: const Icon(Icons.replay),
                    label: Text(s.t('orderAgain')),
                    onPressed: () async {
                      final missing = await busy(context, () => s.reorder(order));
                      if (missing != null && context.mounted) context.read<AppState>().goTab(2);
                    },
                  ),
                ),
            ]),
          ),
        ),
      ),
    );
  }
}
