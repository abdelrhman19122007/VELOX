import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/api.dart';
import '../../core/app_state.dart';
import '../../core/push.dart';
import '../../widgets/common.dart';
import 'inbox_screen.dart';

/// Settings shared by every role: language, dark mode, notifications, password, server link, sign out.
class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});
  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    final push = Push.state;
    return Scaffold(
      appBar: AppBar(title: Text(s.t('settings'))),
      body: ListView(children: [
        if (s.user != null)
          ListTile(
            leading: CircleAvatar(backgroundColor: cs.primaryContainer, child: Text(s.user!.name.characters.first)),
            title: Text(s.user!.name, style: const TextStyle(fontWeight: FontWeight.w800)),
            subtitle: Text(s.user!.email),
          ),
        const Divider(),
        ListTile(
          leading: const Icon(Icons.notifications_outlined),
          title: Text(s.t('notifications')),
          trailing: s.unread > 0 ? Badge(label: Text('${s.unread}')) : null,
          onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const InboxScreen())),
        ),
        SwitchListTile(
          secondary: const Icon(Icons.phone_android),
          title: Text(s.t('pushOn')),
          subtitle: push == PushState.blocked
              ? Text(s.t('pushBlocked'))
              : push == PushState.unavailable
                  ? Text(s.t('pushUnavailable'))
                  : null,
          value: push == PushState.on,
          onChanged: push == PushState.unavailable || push == PushState.on ? null : (_) => s.enablePush().then((_) => setState(() {})),
        ),
        ListTile(leading: const Icon(Icons.translate), title: Text(s.t('language')), onTap: s.toggleLang),
        SwitchListTile(
          secondary: const Icon(Icons.dark_mode_outlined),
          title: Text(s.t('darkMode')),
          value: s.themeMode == ThemeMode.dark,
          onChanged: s.setDark,
        ),
        const Divider(),
        ListTile(leading: const Icon(Icons.password), title: Text(s.t('changePassword')), onTap: () => _changePassword(context)),
        ListTile(leading: const Icon(Icons.dns_outlined), title: Text(s.t('server')), subtitle: Text(apiBase), onTap: () => showServerDialog(context)),
        ListTile(
          leading: const Icon(Icons.info_outline),
          title: Text(s.t('about')),
          subtitle: Text(s.t('aboutBody')),
        ),
        const Divider(),
        ListTile(
          leading: const Icon(Icons.devices),
          title: Text(s.t('logoutAll')),
          onTap: () async {
            if (await confirmDialog(context, '${s.t('logoutAll')}؟')) {
              await s.logoutEverywhere();
              if (context.mounted) Navigator.of(context).popUntil((r) => r.isFirst);
            }
          },
        ),
        ListTile(
          leading: Icon(Icons.logout, color: cs.error),
          title: Text(s.t('logout'), style: TextStyle(color: cs.error, fontWeight: FontWeight.w700)),
          onTap: () {
            s.signOut();
            Navigator.of(context).popUntil((r) => r.isFirst);
          },
        ),
        const SizedBox(height: 24),
      ]),
    );
  }

  Future<void> _changePassword(BuildContext context) async {
    final s = context.read<AppState>();
    final current = TextEditingController(), next = TextEditingController();
    final ok = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: Text(s.t('changePassword')),
        content: Column(mainAxisSize: MainAxisSize.min, children: [
          PasswordField(controller: current, label: s.t('currentPassword')),
          const SizedBox(height: 10),
          PasswordField(controller: next, label: s.t('newPassword'), helper: s.t('passwordHint')),
        ]),
        actions: [
          TextButton(onPressed: () => Navigator.pop(c, false), child: Text(s.t('cancel'))),
          FilledButton(onPressed: () => Navigator.pop(c, true), child: Text(s.t('save'))),
        ],
      ),
    );
    if (ok != true || !context.mounted) return;
    final done = await busy(context, () => s.changePassword(current.text, next.text).then((_) => true));
    if (done == true && context.mounted) showInfo(context, s.t('passwordChanged'));
  }
}

/// Lets the user point the app at another server link (a new public tunnel, a hosted server).
Future<void> showServerDialog(BuildContext context) async {
  final s = context.read<AppState>();
  final c = TextEditingController(text: serverOverride ?? '');
  final ok = await showDialog<bool>(
    context: context,
    builder: (d) => AlertDialog(
      title: Text(s.t('server')),
      content: TextField(
        controller: c,
        keyboardType: TextInputType.url,
        decoration: InputDecoration(hintText: 'https://…', helperText: s.t('serverHint')),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.pop(d, false), child: Text(s.t('cancel'))),
        FilledButton(onPressed: () => Navigator.pop(d, true), child: Text(s.t('save'))),
      ],
    ),
  );
  if (ok == true) await s.setServer(c.text);
}
