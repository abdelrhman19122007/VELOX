import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/app_state.dart';
import '../../core/models.dart';
import '../../widgets/common.dart';
import '../customer/track_screen.dart';

/// The notification inbox (order updates). Every role has one.
class InboxScreen extends StatefulWidget {
  const InboxScreen({super.key});
  @override
  State<InboxScreen> createState() => _InboxScreenState();
}

class _InboxScreenState extends State<InboxScreen> {
  late Future<List<InboxItem>> items;

  @override
  void initState() {
    super.initState();
    items = context.read<AppState>().inbox();
  }

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: Text(s.t('notifications')), actions: [
        TextButton(
          onPressed: () async {
            await busy(context, s.markAllRead);
            setState(() => items = s.inbox());
          },
          child: Text(s.t('markRead')),
        ),
      ]),
      body: FutureBuilder<List<InboxItem>>(
        future: items,
        builder: (c, snap) {
          if (snap.hasError) {
            return EmptyState(Icons.cloud_off, s.errorText(snap.error!),
                action: FilledButton(onPressed: () => setState(() => items = s.inbox()), child: Text(s.t('retry'))));
          }
          if (!snap.hasData) return const Center(child: CircularProgressIndicator());
          final list = snap.data!;
          if (list.isEmpty) return EmptyState(Icons.notifications_none, s.t('empty'));
          return RefreshIndicator(
            onRefresh: () async => setState(() => items = s.inbox()),
            child: ListView.separated(
              itemCount: list.length,
              separatorBuilder: (_, _) => const Divider(),
              itemBuilder: (c, i) {
                final n = list[i];
                return ListTile(
                  leading: CircleAvatar(
                    backgroundColor: n.read ? cs.surfaceContainerHigh : cs.primaryContainer,
                    child: Icon(Icons.receipt_long, color: n.read ? cs.onSurfaceVariant : cs.primary),
                  ),
                  title: Text(n.title, style: TextStyle(fontWeight: n.read ? FontWeight.w500 : FontWeight.w800)),
                  subtitle: Text('${n.body}\n${timeText(context, n.at)}'),
                  isThreeLine: true,
                  onTap: n.orderCode == null || !s.isCustomer
                      ? null
                      : () => Navigator.push(context, MaterialPageRoute(builder: (_) => TrackScreen(code: n.orderCode!))),
                );
              },
            ),
          );
        },
      ),
    );
  }
}
