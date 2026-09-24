import 'package:flutter/material.dart';

import '../core/locale.dart';
import '../main.dart';
import '../services/auth_service.dart';
import '../theme/app_theme.dart';
import '../widgets/glass_card.dart';
import 'main_shell.dart';

class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  @override
  void initState() {
    super.initState();
    _boot();
  }

  Future<void> _boot() async {
    // Restore session + warm the catalog (backend or embedded) in parallel.
    await Future.wait([
      AuthService.instance.restore(),
      CatalogLoading.instance.ensureLoaded(),
    ]);
    // small brand moment
    await Future.delayed(const Duration(milliseconds: 700));
    if (!mounted) return;
    Navigator.of(context).pushReplacement(
      MaterialPageRoute(builder: (_) => const MainShell()),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.canvas,
      body: Stack(
        children: [
          const AmbientGlow(color: AppColors.primary, alignment: Alignment.topRight, size: 420),
          const AmbientGlow(color: AppColors.secondary, alignment: Alignment.bottomLeft, size: 360),
          Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  width: 96,
                  height: 96,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    border: Border.all(color: AppColors.primary, width: 2),
                    boxShadow: [
                      BoxShadow(
                        color: AppColors.primary.withValues(alpha: .4),
                        blurRadius: 40,
                      ),
                    ],
                  ),
                  clipBehavior: Clip.antiAlias,
                  child: Image.asset(
                    'assets/images/velox-logo.jpeg',
                    fit: BoxFit.cover,
                    errorBuilder: (_, __, ___) => const Icon(
                      Icons.bolt,
                      size: 48,
                      color: AppColors.primary,
                    ),
                  ),
                ),
                const SizedBox(height: 24),
                Text(
                  'VELOX',
                  style: Theme.of(context).textTheme.displayMedium?.copyWith(
                        color: AppColors.textHigh,
                        letterSpacing: 6,
                      ),
                ),
                const SizedBox(height: 6),
                Text(
                  AppLocale.tr('app.tagline'),
                  style: const TextStyle(color: AppColors.textMuted),
                ),
                const SizedBox(height: 40),
                const SizedBox(
                  width: 22,
                  height: 22,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: AppColors.primary,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}