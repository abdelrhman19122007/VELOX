import 'package:flutter/material.dart';

import '../theme/app_theme.dart';

/// Primary kinetic button (tangerine gradient + resting glow), or a
/// secondary glass button when [variant] is secondary.
class PrimaryButton extends StatelessWidget {
  final String label;
  final IconData? icon;
  final VoidCallback? onPressed;
  final bool loading;
  final bool fullWidth;
  final dynamic variant; // 'primary' | 'secondary' | 'cyan'

  const PrimaryButton({
    super.key,
    required this.label,
    this.icon,
    this.onPressed,
    this.loading = false,
    this.fullWidth = true,
    this.variant = 'primary',
  });

  @override
  Widget build(BuildContext context) {
    final enabled = onPressed != null && !loading;
    Widget content = Text(
      label,
      style: TextStyle(
        fontSize: 14,
        fontWeight: FontWeight.w700,
        color: variant == 'primary' ? AppColors.onPrimary : AppColors.textHigh,
      ),
    );
    if (loading) {
      content = const SizedBox(
        width: 20,
        height: 20,
        child: CircularProgressIndicator(
          strokeWidth: 2,
          color: AppColors.onPrimary,
        ),
      );
    } else if (icon != null) {
      content = Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          content,
          const SizedBox(width: 8),
          Icon(icon, size: 18, color: Colors.white),
        ],
      );
    }

    final decoration = switch (variant) {
      'secondary' => AppDecor.solid(radius: AppDecor.radiusSm),
      'cyan' => BoxDecoration(
          borderRadius: BorderRadius.circular(AppDecor.radiusSm),
          color: AppColors.secondary.withValues(alpha: .15),
          border: Border.all(color: AppColors.secondary),
        ),
      _ => AppDecor.primaryGlow(radius: AppDecor.radiusSm),
    };

    final btn = AnimatedOpacity(
      duration: const Duration(milliseconds: 150),
      opacity: enabled ? 1 : .55,
      child: Material(
        color: Colors.transparent,
        child: Ink(
          decoration: decoration,
          child: InkWell(
            borderRadius: BorderRadius.circular(AppDecor.radiusSm),
            onTap: enabled ? onPressed : null,
            child: Container(
              height: 50,
              alignment: Alignment.center,
              child: content,
            ),
          ),
        ),
      ),
    );

    return fullWidth ? SizedBox(width: double.infinity, child: btn) : btn;
  }
}