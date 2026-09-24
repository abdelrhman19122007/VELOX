import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

/// "Kinetic Obsidian" — deep midnight glassmorphism with cyber tangerine /
/// electric cyan accents (see DESIGN.md shipped with the app).
class AppColors {
  AppColors._();

  // Canvas
  static const canvas = Color(0xFF0B0F19); // midnight slate
  static const surface = Color(0xFF0F131D);
  static const surfaceLow = Color(0xFF121826);
  static const surfaceHigh = Color(0xFF161F30);
  static const glass = Color(0xA6141A28); // rgba(20,26,40,0.65)

  // Accents
  static const primary = Color(0xFFFF5E1E); // cyber tangerine
  static const primaryDeep = Color(0xFFFF2E00); // solar red gradient end
  static const onPrimary = Color(0xFFFFFFFF);
  static const secondary = Color(0xFF00E5FF); // electric cyan
  static const tertiary = Color(0xFF7928CA); // hyper violet

  // Text
  static const textHigh = Color(0xFFF8FAFC);
  static const textMuted = Color(0xFF94A3B8);
  static const textDisabled = Color(0xFF475569);

  // Strokes & glows
  static const border = Color(0x14FFFFFF); // rgba(255,255,255,0.08)
  static const borderStrong = Color(0x26FFFFFF); // rgba(255,255,255,0.15)
  static const primaryBorder = Color(0x66FF5E1E); // rgba(255,94,30,0.4)
  static const cyanBorder = Color(0xFF00E5FF);

  static const success = Color(0xFF34D399);
  static const danger = Color(0xFFFF6B6B);
  static const warn = Color(0xFFFBBF24);
}

/// Shared glass-card + glow box decorations.
class AppDecor {
  AppDecor._();

  static const double radiusSm = 8;
  static const double radiusMd = 16;
  static const double radiusLg = 24;

  static BoxDecoration glass({double radius = radiusMd, Color? borderColor}) =>
      BoxDecoration(
        color: AppColors.glass,
        borderRadius: BorderRadius.circular(radius),
        border: Border.all(color: borderColor ?? AppColors.border),
      );

  static BoxDecoration solid(
          {double radius = radiusMd, Color color = AppColors.surfaceLow}) =>
      BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(radius),
        border: Border.all(color: AppColors.border),
      );

  static BoxDecoration primaryGlow({double radius = radiusMd}) =>
      BoxDecoration(
        borderRadius: BorderRadius.circular(radius),
        gradient: const LinearGradient(
          begin: Alignment.centerLeft,
          end: Alignment.centerRight,
          colors: [AppColors.primary, AppColors.primaryDeep],
        ),
        boxShadow: [
          BoxShadow(
            color: AppColors.primary.withValues(alpha: .35),
            blurRadius: 16,
            offset: const Offset(0, 4),
          ),
        ],
      );

  /// Promo banner — tertiary hyper violet bleeding into midnight.
  static BoxDecoration promoBanner() => BoxDecoration(
        borderRadius: BorderRadius.circular(radiusLg),
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFF1B1030), Color(0xFF0E1420)],
        ),
        border: Border.all(color: AppColors.tertiary.withValues(alpha: .4)),
        boxShadow: [
          BoxShadow(
            color: AppColors.tertiary.withValues(alpha: .25),
            blurRadius: 30,
          ),
        ],
      );
}

ThemeData buildVeloxTheme({required String lang}) {
  final base = ThemeData(
    useMaterial3: true,
    brightness: Brightness.dark,
    scaffoldBackgroundColor: AppColors.canvas,
    colorScheme: const ColorScheme.dark(
      primary: AppColors.primary,
      onPrimary: AppColors.onPrimary,
      secondary: AppColors.secondary,
      onSecondary: Color(0xFF00363D),
      tertiary: AppColors.tertiary,
      onTertiary: Colors.white,
      surface: AppColors.surface,
      onSurface: AppColors.textHigh,
      error: AppColors.danger,
      onError: Colors.white,
    ),
    textTheme: GoogleFonts.interTextTheme(
      ThemeData.dark().textTheme.apply(
            bodyColor: AppColors.textHigh,
            displayColor: AppColors.textHigh,
          ),
    ).copyWith(
      displayLarge: GoogleFonts.spaceGrotesk(
        fontSize: 36,
        fontWeight: FontWeight.w700,
        color: AppColors.textHigh,
        letterSpacing: -0.02,
      ),
      displayMedium: GoogleFonts.spaceGrotesk(
        fontSize: 28,
        fontWeight: FontWeight.w700,
        color: AppColors.textHigh,
      ),
      headlineMedium: GoogleFonts.spaceGrotesk(
        fontSize: 22,
        fontWeight: FontWeight.w600,
        color: AppColors.textHigh,
      ),
      headlineSmall: GoogleFonts.spaceGrotesk(
        fontSize: 18,
        fontWeight: FontWeight.w600,
        color: AppColors.textHigh,
      ),
      titleLarge: GoogleFonts.inter(
        fontSize: 18,
        fontWeight: FontWeight.w600,
        color: AppColors.textHigh,
      ),
      titleMedium: GoogleFonts.inter(
        fontSize: 16,
        fontWeight: FontWeight.w600,
        color: AppColors.textHigh,
      ),
      bodyLarge: const TextStyle(fontSize: 16, color: AppColors.textHigh),
      bodyMedium: const TextStyle(fontSize: 14, color: AppColors.textHigh),
      bodySmall: const TextStyle(fontSize: 12, color: AppColors.textMuted),
      labelLarge: const TextStyle(
        fontSize: 14,
        fontWeight: FontWeight.w600,
        color: AppColors.textHigh,
        letterSpacing: 0.01,
      ),
      labelSmall: const TextStyle(
        fontSize: 10,
        fontWeight: FontWeight.w700,
        color: AppColors.textMuted,
        letterSpacing: 0.06,
      ),
    ),
    appBarTheme: AppBarTheme(
      backgroundColor: AppColors.canvas,
      elevation: 0,
      scrolledUnderElevation: 0,
      iconTheme: const IconThemeData(color: AppColors.textHigh),
      titleTextStyle: GoogleFonts.spaceGrotesk(
        fontSize: 20,
        fontWeight: FontWeight.w700,
        color: AppColors.textHigh,
      ),
    ),
    bottomNavigationBarTheme: const BottomNavigationBarThemeData(
      backgroundColor: AppColors.surfaceLow,
      selectedItemColor: AppColors.primary,
      unselectedItemColor: AppColors.textDisabled,
      type: BottomNavigationBarType.fixed,
      elevation: 0,
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: const Color(0xFF0E1422),
      hintStyle: const TextStyle(color: AppColors.textDisabled),
      labelStyle: const TextStyle(color: AppColors.textMuted),
      contentPadding:
          const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(AppDecor.radiusSm),
        borderSide: const BorderSide(color: AppColors.border),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(AppDecor.radiusSm),
        borderSide: const BorderSide(color: AppColors.border),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(AppDecor.radiusSm),
        borderSide: const BorderSide(color: AppColors.secondary, width: 1.2),
      ),
      errorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(AppDecor.radiusSm),
        borderSide: const BorderSide(color: AppColors.danger),
      ),
    ),
    chipTheme: ChipThemeData(
      backgroundColor: Colors.white.withValues(alpha: .04),
      selectedColor: AppColors.surfaceLow,
      side: const BorderSide(color: AppColors.border),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(999),
      ),
      labelStyle: const TextStyle(color: AppColors.textHigh, fontSize: 13),
    ),
    snackBarTheme: SnackBarThemeData(
      backgroundColor: AppColors.surfaceHigh,
      contentTextStyle: const TextStyle(color: AppColors.textHigh),
      behavior: SnackBarBehavior.floating,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppDecor.radiusMd),
      ),
    ),
    dividerTheme: const DividerThemeData(
      color: AppColors.border,
      thickness: 1,
    ),
    progressIndicatorTheme: const ProgressIndicatorThemeData(
      color: AppColors.primary,
    ),
    floatingActionButtonTheme: const FloatingActionButtonThemeData(
      backgroundColor: AppColors.primary,
      foregroundColor: AppColors.onPrimary,
    ),
    pageTransitionsTheme: const PageTransitionsTheme(builders: {
      TargetPlatform.android: ZoomPageTransitionsBuilder(),
      TargetPlatform.iOS: CupertinoPageTransitionsBuilder(),
      TargetPlatform.windows: ZoomPageTransitionsBuilder(),
      TargetPlatform.macOS: CupertinoPageTransitionsBuilder(),
      TargetPlatform.linux: ZoomPageTransitionsBuilder(),
    }),
  );

  // Arabic typography parity (Cairo is the geometric pairing for Space Grotesk).
  if (lang == 'ar') {
    return base.copyWith(
      textTheme: base.textTheme.copyWith(
        displayLarge: GoogleFonts.cairo(
          fontSize: 36,
          fontWeight: FontWeight.w700,
          color: AppColors.textHigh,
        ),
        displayMedium: GoogleFonts.cairo(
          fontSize: 28,
          fontWeight: FontWeight.w700,
          color: AppColors.textHigh,
        ),
        headlineMedium: GoogleFonts.cairo(
          fontSize: 22,
          fontWeight: FontWeight.w700,
          color: AppColors.textHigh,
        ),
        headlineSmall: GoogleFonts.cairo(
          fontSize: 18,
          fontWeight: FontWeight.w700,
          color: AppColors.textHigh,
        ),
        titleLarge: GoogleFonts.cairo(
          fontSize: 18,
          fontWeight: FontWeight.w700,
          color: AppColors.textHigh,
        ),
        titleMedium: GoogleFonts.cairo(
          fontSize: 16,
          fontWeight: FontWeight.w700,
          color: AppColors.textHigh,
        ),
        bodyLarge: const TextStyle(fontSize: 16, color: AppColors.textHigh),
        bodyMedium: const TextStyle(fontSize: 14, color: AppColors.textHigh),
      ),
    );
  }
  return base;
}