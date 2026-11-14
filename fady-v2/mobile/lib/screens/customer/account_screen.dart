import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/app_state.dart';
import '../../core/models.dart';
import '../../widgets/common.dart';
import '../shared/inbox_screen.dart';
import '../shared/settings_screen.dart';

/// The customer's account: wallet and points, notifications, addresses, complaints and settings.
class AccountScreen extends StatelessWidget {
  const AccountScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    void open(Widget w) => Navigator.push(context, MaterialPageRoute(builder: (_) => w));
    return Scaffold(
      appBar: AppBar(title: Text(s.t('account'))),
      body: ListView(children: [
        ListTile(
          leading: CircleAvatar(radius: 26, backgroundColor: cs.primaryContainer, child: Text(s.user!.name.characters.first, style: const TextStyle(fontSize: 20))),
          title: Text(s.user!.name, style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 18)),
          subtitle: Text('${s.user!.email}\n${s.user!.phone ?? ''}'),
          isThreeLine: true,
        ),
        const Divider(),
        ListTile(leading: const Icon(Icons.account_balance_wallet_outlined), title: Text(s.t('wallet')), trailing: const Icon(Icons.chevron_right), onTap: () => open(const WalletScreen())),
        ListTile(
          leading: const Icon(Icons.notifications_outlined),
          title: Text(s.t('notifications')),
          trailing: s.unread > 0 ? Badge(label: Text('${s.unread}')) : const Icon(Icons.chevron_right),
          onTap: () => open(const InboxScreen()),
        ),
        ListTile(leading: const Icon(Icons.location_on_outlined), title: Text(s.t('savedAddresses')), trailing: const Icon(Icons.chevron_right), onTap: () => open(const AddressesScreen())),
        ListTile(leading: const Icon(Icons.support_agent), title: Text(s.t('complaints')), trailing: const Icon(Icons.chevron_right), onTap: () => open(const ComplaintsScreen())),
        ListTile(leading: const Icon(Icons.settings_outlined), title: Text(s.t('settings')), trailing: const Icon(Icons.chevron_right), onTap: () => open(const SettingsScreen())),
      ]),
    );
  }
}

class WalletScreen extends StatefulWidget {
  const WalletScreen({super.key});
  @override
  State<WalletScreen> createState() => _WalletScreenState();
}

class _WalletScreenState extends State<WalletScreen> {
  late Future<Wallet> wallet;

  @override
  void initState() {
    super.initState();
    wallet = context.read<AppState>().wallet();
  }

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: Text(s.t('wallet'))),
      body: FutureBuilder<Wallet>(
        future: wallet,
        builder: (c, snap) {
          if (snap.hasError) return EmptyState(Icons.cloud_off, s.errorText(snap.error!));
          if (!snap.hasData) return const Center(child: CircularProgressIndicator());
          final w = snap.data!;
          return ListView(padding: const EdgeInsets.all(16), children: [
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                gradient: LinearGradient(colors: [cs.primary, cs.primary.withValues(alpha: 0.8)]),
                borderRadius: BorderRadius.circular(20),
              ),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(s.t('balance'), style: TextStyle(color: cs.onPrimary.withValues(alpha: 0.8))),
                Text(s.money(w.balance), style: TextStyle(color: cs.onPrimary, fontSize: 32, fontWeight: FontWeight.w900)),
                const SizedBox(height: 8),
                Text('⭐ ${w.points} ${s.t('points')}', style: TextStyle(color: cs.onPrimary, fontWeight: FontWeight.w700)),
                Text(s.t('pointsNote'), style: TextStyle(color: cs.onPrimary.withValues(alpha: 0.8), fontSize: 12.5)),
              ]),
            ),
            const SizedBox(height: 10),
            OutlinedButton.icon(
              icon: const Icon(Icons.add),
              label: Text(s.t('topUp')),
              onPressed: () async {
                final r = await busy(context, () => s.topUp(100));
                if (r != null) setState(() => wallet = Future.value(r));
              },
            ),
            const SizedBox(height: 10),
            if (w.history.isEmpty) EmptyState(Icons.receipt_outlined, s.t('empty')),
            for (final t in w.history)
              ListTile(
                leading: Icon(t.amount >= 0 ? Icons.arrow_downward : Icons.arrow_upward, color: t.amount >= 0 ? cs.secondary : cs.error),
                title: Text(s.t('tx_${t.reason}')),
                subtitle: Text(timeText(context, t.at)),
                trailing: Text('${t.amount >= 0 ? '+' : ''}${s.money(t.amount)}',
                    style: TextStyle(fontWeight: FontWeight.w800, color: t.amount >= 0 ? cs.secondary : cs.error)),
              ),
          ]);
        },
      ),
    );
  }
}

class AddressesScreen extends StatelessWidget {
  const AddressesScreen({super.key});
  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    return Scaffold(
      appBar: AppBar(title: Text(s.t('savedAddresses'))),
      body: s.addresses.isEmpty
          ? EmptyState(Icons.location_off_outlined, s.t('empty'))
          : ListView(children: [
              for (final a in s.addresses)
                ListTile(
                  leading: const Icon(Icons.bookmark_outline),
                  title: Text('${a.label} · ${s.zoneName(a.zone)}', style: const TextStyle(fontWeight: FontWeight.w800)),
                  subtitle: Text('${a.address}${a.landmark == null ? '' : '\n🏁 ${a.landmark}'}\n${a.phone}'),
                  isThreeLine: true,
                  trailing: IconButton(
                    icon: const Icon(Icons.delete_outline),
                    onPressed: () => busy(context, () => s.deleteAddress(a.id)),
                  ),
                ),
            ]),
    );
  }
}

class ComplaintsScreen extends StatelessWidget {
  const ComplaintsScreen({super.key});
  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: Text(s.t('complaints'))),
      body: FutureBuilder<List<Complaint>>(
        future: s.myComplaints(),
        builder: (c, snap) {
          if (snap.hasError) return EmptyState(Icons.cloud_off, s.errorText(snap.error!));
          if (!snap.hasData) return const Center(child: CircularProgressIndicator());
          if (snap.data!.isEmpty) return EmptyState(Icons.sentiment_satisfied_alt, s.t('empty'));
          return ListView(children: [
            for (final x in snap.data!)
              ListTile(
                leading: Icon(x.status == 'OPEN' ? Icons.hourglass_top : Icons.check_circle, color: x.status == 'OPEN' ? cs.tertiary : cs.secondary),
                title: Text('${x.code} · ${x.orderCode}', style: const TextStyle(fontWeight: FontWeight.w800)),
                subtitle: Text([
                  x.details,
                  if (x.adminNote != null) '💬 ${x.adminNote}',
                  if (x.compensationCode != null) '🎁 ${x.compensationCode}',
                ].join('\n')),
                isThreeLine: true,
              ),
          ]);
        },
      ),
    );
  }
}
