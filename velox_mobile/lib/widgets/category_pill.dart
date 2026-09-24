import 'package:flutter/material.dart';

import '../core/locale.dart';
import '../theme/app_theme.dart';

/// Capsule category chip. Active state = obsidian fill + cyan border
/// + cyan status dot (per the design system).
class CategoryPill extends StatelessWidget {
  final String id;
  final String title;
  final bool active;
  final VoidCallback onTap;

  const CategoryPill({
    super.key,
    required this.id,
    required this.title,
    required this.active,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final bg = active
        ? AppColors.surfaceLow
        : Colors.white.withValues(alpha: .04);
    final border = active
        ? Border.all(color: AppColors.secondary)
        : Border.all(color: AppColors.border);
    final textColor = active ? AppColors.secondary : AppColors.textMuted;

    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 9),
        decoration: BoxDecoration(
          color: bg,
          borderRadius: BorderRadius.circular(999),
          border: border,
          boxShadow: active
              ? [
                  BoxShadow(
                    color: AppColors.secondary.withValues(alpha: .15),
                    blurRadius: 10,
                  )
                ]
              : null,
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (active) ...[
              Container(
                width: 6,
                height: 6,
                decoration: const BoxDecoration(
                  color: AppColors.secondary,
                  shape: BoxShape.circle,
                ),
              ),
              const SizedBox(width: 7),
            ],
            Text(
              title,
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: textColor,
                fontFamily:
                    AppLocale.isAr ? 'Cairo' : null,
              ),
            ),
          ],
        ),
      ),
    );
  }
}