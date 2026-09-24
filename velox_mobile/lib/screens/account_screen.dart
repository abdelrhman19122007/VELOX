import 'package:flutter/material.dart';

import '../core/locale.dart';
import '../services/auth_service.dart';
import '../theme/app_theme.dart';
import '../widgets/empty_state.dart';
import '../widgets/glass_card.dart';
import 'auth/login_screen.dart';
import 'orders_screen.dart';
import 'settings_screen.dart';

class AccountScreen extends StatelessWidget {
  const AccountScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final profile = AuthService.instance.profile;

    return Scaffold(
      appBar: AppBar(title: Text(AppLocale.tr('acct.title'))),
      body: profile == null
          ? EmptyState(
              icon: Icons.person_outline,
              title: AppLocale.tr('acct.notLoggedIn'),
              hint: AppLocale.tr('acct.loginPrompt'),
              actionLabel: AppLocale.tr('auth.login'),
              onAction: () => Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => const LoginScreen()),
              ),
            )
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                // profile header
                GlassCard(
                  padding: const EdgeInsets.all(18),
                  color: AppColors.surfaceLow,
                  child: Row(
                    children: [
                      Container(
                        width: 62,
                        height: 62,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          gradient: const LinearGradient(
                            colors: [
                              AppColors.primary,
                              AppColors.primaryDeep,
                            ],
                          ),
                        ),
                        alignment: Alignment.center,
                        child: Text(
                          profile.fullName.isNotEmpty
                              ? profile.fullName.characters.first.toUpperCase()
                              : 'V',
                          style: const TextStyle(
                              fontSize: 26,
                              fontWeight: FontWeight.w800,
                              color: Colors.white),
                        ),
                      ),
                      const SizedBox(width: 16),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              profile.fullName,
                              style: Theme.of(context)
                                  .textTheme
                                  .titleLarge
                                  ?.copyWith(fontSize: 17),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              profile.email,
                              style: const TextStyle(
                                  fontSize: 12, color: AppColors.textMuted),
                            ),
                            const SizedBox(height: 6),
                            Container(
                              padding: const EdgeInsets.symmetric(
                                  horizontal: 10, vertical: 3),
                              decoration: BoxDecoration(
                                color:
                                    AppColors.secondary.withValues(alpha: .12),
                                borderRadius: BorderRadius.circular(999),
                                border:
                                    Border.all(color: AppColors.secondary),
                              ),
                              child: Text(
                                profile.governorate,
                                style: const TextStyle(
                                    fontSize: 10,
                                    fontWeight: FontWeight.w700,
                                    color: AppColors.secondary),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 16),
                _MenuTile(
                  icon: Icons.receipt_long_outlined,
                  title: AppLocale.tr('acct.orders'),
                  onTap: () => Navigator.of(context).push(
                    MaterialPageRoute(builder: (_) => const OrdersScreen()),
                  ),
                ),
                _MenuTile(
                  icon: Icons.settings_outlined,
                  title: AppLocale.tr('acct.settings'),
                  onTap: () => Navigator.of(context).push(
                    MaterialPageRoute(builder: (_) => const SettingsScreen()),
                  ),
                ),
                _MenuTile(
                  icon: Icons.location_on_outlined,
                  title: AppLocale.tr('acct.address'),
                  onTap: () => _coming(context),
                ),
                _MenuTile(
                  icon: Icons.favorite_outline,
                  title: AppLocale.tr('acct.favorites'),
                  onTap: () => _coming(context),
                ),
                _MenuTile(
                  icon: Icons.notifications_outlined,
                  title: AppLocale.tr('acct.notifications'),
                  onTap: () => _coming(context),
                ),
                _MenuTile(
                  icon: Icons.workspace_premium_outlined,
                  title: AppLocale.tr('acct.plus'),
                  onTap: () => _coming(context),
                ),
                _MenuTile(
                  icon: Icons.card_giftcard_outlined,
                  title: AppLocale.tr('acct.rewards'),
                  onTap: () => _coming(context),
                ),
                _MenuTile(
                  icon: Icons.headset_mic_outlined,
                  title: AppLocale.tr('acct.support'),
                  onTap: () => _coming(context),
                ),
                const SizedBox(height: 8),
                Center(
                  child: Text(
                    AppLocale.tr('acct.version'),
                    style: const TextStyle(
                        fontSize: 11, color: AppColors.textDisabled),
                  ),
                ),
              ],
            ),
    );
  }

  void _coming(BuildContext context) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(
        content: Text(AppLocale.t('قريباً في التحديث القادم', 'Coming soon')),
        duration: const Duration(seconds: 1),
      ));
  }
}

class _MenuTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final VoidCallback onTap;

  const _MenuTile({
    required this.icon,
    required this.title,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: GlassCard(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 15),
        color: AppColors.surfaceLow,
        onTap: onTap,
        child: Row(
          children: [
            Icon(icon, size: 20, color: AppColors.primary),
            const SizedBox(width: 14),
            Expanded(
              child: Text(title,
                  style: const TextStyle(
                      fontSize: 14, fontWeight: FontWeight.w600)),
            ),
            const Icon(Icons.chevron_left,
                size: 18, color: AppColors.textDisabled),
          ],
        ),
      ),
    );
  }
}