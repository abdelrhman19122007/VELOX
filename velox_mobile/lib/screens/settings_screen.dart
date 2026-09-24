import 'package:flutter/material.dart';

import '../core/api_client.dart';
import '../core/locale.dart';
import '../main.dart';
import '../services/auth_service.dart';
import '../theme/app_theme.dart';
import '../widgets/glass_card.dart';
import '../widgets/primary_button.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  final _phoneCtrl = TextEditingController();
  bool _saving = false;
  String? _msg;
  bool _error = false;

  @override
  void initState() {
    super.initState();
    final p = AuthService.instance.profile;
    if (p != null) _phoneCtrl.text = p.phone;
  }

  @override
  void dispose() {
    _phoneCtrl.dispose();
    super.dispose();
  }

  Future<void> _savePhone() async {
    final phone = _phoneCtrl.text.replaceAll(' ', '');
    if (!RegExp(r'^01\d{9}$').hasMatch(phone)) {
      setState(() {
        _error = true;
        _msg = AppLocale.t('رقم مصري صحيح (01xxxxxxxxx)',
            'Valid Egyptian number (01xxxxxxxxx)');
      });
      return;
    }
    setState(() {
      _saving = true;
      _msg = null;
    });
    try {
      await AuthService.instance.updatePhone(phone);
      AuthServiceState.instance.notify();
      if (!mounted) return;
      setState(() {
        _saving = false;
        _error = false;
        _msg = AppLocale.tr('set.saved');
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _saving = false;
        _error = true;
        _msg = e is ApiException ? e.message : '$e';
      });
    }
  }

  Future<void> _confirmLogout() async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        backgroundColor: AppColors.surfaceLow,
        title: Text(AppLocale.tr('set.logoutTitle')),
        content: Text(AppLocale.tr('set.logoutConfirm')),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: Text(AppLocale.t('إلغاء', 'Cancel')),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: Text(
              AppLocale.tr('set.logoutTitle'),
              style: const TextStyle(color: AppColors.danger),
            ),
          ),
        ],
      ),
    );
    if (ok != true) return;
    await AuthService.instance.logout();
    AuthServiceState.instance.notify();
    if (!mounted) return;
    Navigator.of(context).popUntil((r) => r.isFirst);
  }

  @override
  Widget build(BuildContext context) {
    final ar = AppLocale.isAr;
    return Scaffold(
      appBar: AppBar(title: Text(AppLocale.tr('acct.settings'))),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Phone (client fix requirement: settings is where phone editing lives)
          GlassCard(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const Icon(Icons.phone_android,
                        size: 18, color: AppColors.primary),
                    const SizedBox(width: 8),
                    Text(AppLocale.tr('set.phone'),
                        style: Theme.of(context).textTheme.titleMedium),
                  ],
                ),
                const SizedBox(height: 4),
                Text(
                  AppLocale.tr('set.phoneHint'),
                  style: const TextStyle(
                      fontSize: 12, color: AppColors.textMuted),
                ),
                const SizedBox(height: 14),
                TextField(
                  controller: _phoneCtrl,
                  keyboardType: TextInputType.phone,
                  style: const TextStyle(color: AppColors.textHigh),
                  decoration: InputDecoration(
                    prefixIcon: const Icon(Icons.call_outlined,
                        color: AppColors.textMuted),
                  ),
                ),
                if (_msg != null) ...[
                  const SizedBox(height: 10),
                  Text(
                    _msg!,
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                      color: _error ? AppColors.danger : AppColors.success,
                    ),
                  ),
                ],
                const SizedBox(height: 14),
                PrimaryButton(
                  label: AppLocale.tr('set.save'),
                  icon: Icons.save_outlined,
                  loading: _saving,
                  onPressed: _saving ? null : _savePhone,
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          // Language
          GlassCard(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const Icon(Icons.language,
                        size: 18, color: AppColors.primary),
                    const SizedBox(width: 8),
                    Text(AppLocale.tr('set.language'),
                        style: Theme.of(context).textTheme.titleMedium),
                  ],
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      child: _LangTile(
                        label: AppLocale.tr('set.arabic'),
                        selected: ar,
                        onTap: () {
                          if (!ar) AuthServiceState.instance.setLang('ar');
                        },
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: _LangTile(
                        label: AppLocale.tr('set.english'),
                        selected: !ar,
                        onTap: () {
                          if (ar) AuthServiceState.instance.setLang('en');
                        },
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          // Logout
          if (AuthService.instance.isLoggedIn)
            GlassCard(
              padding: EdgeInsets.zero,
              color: AppColors.surfaceLow,
              borderColor: AppColors.danger.withValues(alpha: .3),
              onTap: _confirmLogout,
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 16),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Icon(Icons.logout, size: 18, color: AppColors.danger),
                    const SizedBox(width: 8),
                    Text(
                      AppLocale.tr('auth.logout'),
                      style: const TextStyle(
                          color: AppColors.danger,
                          fontWeight: FontWeight.w700),
                    ),
                  ],
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _LangTile extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;

  const _LangTile({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(10),
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 12),
          decoration: BoxDecoration(
            color: selected
                ? AppColors.secondary.withValues(alpha: .12)
                : AppColors.surfaceHigh,
            borderRadius: BorderRadius.circular(10),
            border: Border.all(
                color: selected ? AppColors.secondary : AppColors.border),
          ),
          child: Column(
            children: [
              Icon(
                selected
                    ? Icons.radio_button_checked
                    : Icons.radio_button_off,
                size: 16,
                color: selected ? AppColors.secondary : AppColors.textDisabled,
              ),
              const SizedBox(height: 6),
              Text(
                label,
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w700,
                  color: selected ? AppColors.secondary : AppColors.textMuted,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}