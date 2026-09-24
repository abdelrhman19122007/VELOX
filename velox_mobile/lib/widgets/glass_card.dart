import 'package:flutter/material.dart';

import '../theme/app_theme.dart';

/// Reusable glassmorphic container (Tier 2 in the design system).
class GlassCard extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry padding;
  final double radius;
  final Color? color;
  final Color? borderColor;
  final VoidCallback? onTap;
  final double? width;
  final double? height;

  const GlassCard({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.all(16),
    this.radius = AppDecor.radiusMd,
    this.color,
    this.borderColor,
    this.onTap,
    this.width,
    this.height,
  });

  @override
  Widget build(BuildContext context) {
    final decoration = BoxDecoration(
      color: color ?? AppColors.glass,
      borderRadius: BorderRadius.circular(radius),
      border: Border.all(color: borderColor ?? AppColors.border),
    );
    return Container(
      width: width,
      height: height,
      decoration: decoration,
      child: onTap == null
          ? Padding(padding: padding, child: child)
          : Material(
              color: Colors.transparent,
              borderRadius: BorderRadius.circular(radius),
              child: InkWell(
                borderRadius: BorderRadius.circular(radius),
                onTap: onTap,
                child: Padding(padding: padding, child: child),
              ),
            ),
    );
  }
}

/// Ambient background glows used on canvas-level screens.
class AmbientGlow extends StatelessWidget {
  final Color color;
  final Alignment alignment;
  final double size;

  const AmbientGlow({
    super.key,
    required this.color,
    this.alignment = Alignment.topRight,
    this.size = 300,
  });

  @override
  Widget build(BuildContext context) {
    return IgnorePointer(
      child: Align(
        alignment: alignment,
        child: Container(
          width: size,
          height: size,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            gradient: RadialGradient(
              colors: [
                color.withValues(alpha: .14),
                color.withValues(alpha: 0),
              ],
            ),
          ),
        ),
      ),
    );
  }
}