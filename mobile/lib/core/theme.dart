import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

/// "Nile Terracotta" — the design system from the team's Stitch design (design/DESIGN_SYSTEM.md):
/// warm terracotta for actions, Nile green for success and "open", a soft clay background.
/// Same tokens as the web app (web/css/tokens.css).
class VColors {
  static const terracotta = Color(0xFF9C3D10);
  static const terracottaLight = Color(0xFFFFB693);
  static const nile = Color(0xFF1F6F4A);
  static const nileLight = Color(0xFF7FD9A8);
  static const sand = Color(0xFFFFF8F5);
  static const ink = Color(0xFF231914);
  static const amber = Color(0xFFB26A00);
  static const danger = Color(0xFFBA1A1A);
}

ThemeData buildTheme(Brightness b, String lang) {
  final dark = b == Brightness.dark;
  final scheme = ColorScheme(
    brightness: b,
    primary: dark ? VColors.terracottaLight : VColors.terracotta,
    onPrimary: dark ? const Color(0xFF5A1B00) : Colors.white,
    primaryContainer: dark ? const Color(0xFF7A2E06) : const Color(0xFFFFDBCC),
    onPrimaryContainer: dark ? const Color(0xFFFFDBCC) : const Color(0xFF380D00),
    secondary: dark ? VColors.nileLight : VColors.nile,
    onSecondary: dark ? const Color(0xFF003920) : Colors.white,
    secondaryContainer: dark ? const Color(0xFF005234) : const Color(0xFFB3F1CC),
    onSecondaryContainer: dark ? const Color(0xFFB3F1CC) : const Color(0xFF002111),
    tertiary: VColors.amber,
    onTertiary: Colors.white,
    error: dark ? const Color(0xFFFFB4AB) : VColors.danger,
    onError: dark ? const Color(0xFF690005) : Colors.white,
    surface: dark ? const Color(0xFF1A110D) : VColors.sand,
    onSurface: dark ? const Color(0xFFF1DFD8) : VColors.ink,
    surfaceContainerLowest: dark ? const Color(0xFF140C09) : Colors.white,
    surfaceContainerLow: dark ? const Color(0xFF231915) : const Color(0xFFFFF1EB),
    surfaceContainer: dark ? const Color(0xFF271D19) : const Color(0xFFFCEAE3),
    surfaceContainerHigh: dark ? const Color(0xFF322823) : const Color(0xFFF6E4DD),
    surfaceContainerHighest: dark ? const Color(0xFF3D322D) : const Color(0xFFF1DFD8),
    onSurfaceVariant: dark ? const Color(0xFFD8C2BA) : const Color(0xFF53433C),
    outline: dark ? const Color(0xFFA08D85) : const Color(0xFF85736B),
    outlineVariant: dark ? const Color(0xFF53433C) : const Color(0xFFE6D3CB),
  );
  final base = ThemeData(colorScheme: scheme, useMaterial3: true, brightness: b);
  // Cairo reads well in Arabic at small sizes; minimum body size 14 for older users.
  final text = GoogleFonts.cairoTextTheme(base.textTheme).copyWith(
    bodyMedium: GoogleFonts.cairo(fontSize: 14.5, height: 1.45),
    bodySmall: GoogleFonts.cairo(fontSize: 13, height: 1.4),
    labelSmall: GoogleFonts.cairo(fontSize: 12, fontWeight: FontWeight.w600),
  );
  final radius = BorderRadius.circular(14);
  return base.copyWith(
    // 10 % larger than Material's defaults: the team found the text small on phones and laptops
    textTheme: text.apply(bodyColor: scheme.onSurface, displayColor: scheme.onSurface, fontSizeFactor: 1.1),
    scaffoldBackgroundColor: scheme.surface,
    appBarTheme: AppBarTheme(
      backgroundColor: scheme.surface,
      foregroundColor: scheme.onSurface,
      elevation: 0,
      scrolledUnderElevation: 0.5,
      centerTitle: false,
      titleTextStyle: GoogleFonts.cairo(fontSize: 19, fontWeight: FontWeight.w800, color: scheme.onSurface),
    ),
    cardTheme: CardThemeData(
      color: scheme.surfaceContainerLowest,
      elevation: 0,
      margin: EdgeInsets.zero,
      clipBehavior: Clip.antiAlias,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18), side: BorderSide(color: scheme.outlineVariant)),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        minimumSize: const Size(48, 52),
        shape: RoundedRectangleBorder(borderRadius: radius),
        textStyle: GoogleFonts.cairo(fontSize: 16, fontWeight: FontWeight.w700),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        minimumSize: const Size(48, 48),
        shape: RoundedRectangleBorder(borderRadius: radius),
        textStyle: GoogleFonts.cairo(fontSize: 15, fontWeight: FontWeight.w700),
      ),
    ),
    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(textStyle: GoogleFonts.cairo(fontSize: 15, fontWeight: FontWeight.w700)),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: scheme.surfaceContainerLowest,
      border: OutlineInputBorder(borderRadius: radius, borderSide: BorderSide(color: scheme.outlineVariant)),
      enabledBorder: OutlineInputBorder(borderRadius: radius, borderSide: BorderSide(color: scheme.outlineVariant)),
      focusedBorder: OutlineInputBorder(borderRadius: radius, borderSide: BorderSide(color: scheme.primary, width: 1.8)),
      contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 15),
    ),
    chipTheme: base.chipTheme.copyWith(
      selectedColor: scheme.primaryContainer,
      checkmarkColor: scheme.onPrimaryContainer,
      shape: const StadiumBorder(),
      side: BorderSide(color: scheme.outlineVariant),
      labelStyle: GoogleFonts.cairo(fontSize: 14, fontWeight: FontWeight.w600, color: scheme.onSurface),
    ),
    navigationBarTheme: NavigationBarThemeData(
      backgroundColor: scheme.surfaceContainerLowest,
      indicatorColor: scheme.primaryContainer,
      height: 68,
      labelTextStyle: WidgetStatePropertyAll(GoogleFonts.cairo(fontSize: 12.5, fontWeight: FontWeight.w700)),
    ),
    segmentedButtonTheme: SegmentedButtonThemeData(
      style: ButtonStyle(
        backgroundColor: WidgetStateProperty.resolveWith((st) => st.contains(WidgetState.selected) ? scheme.primaryContainer : null),
        foregroundColor: WidgetStateProperty.resolveWith((st) => st.contains(WidgetState.selected) ? scheme.onPrimaryContainer : scheme.onSurface),
        textStyle: WidgetStatePropertyAll(GoogleFonts.cairo(fontSize: 15, fontWeight: FontWeight.w700)),
      ),
    ),
    snackBarTheme: const SnackBarThemeData(behavior: SnackBarBehavior.floating),
    dividerTheme: DividerThemeData(color: scheme.outlineVariant, space: 1),
    bottomSheetTheme: BottomSheetThemeData(backgroundColor: scheme.surface, showDragHandle: true),
  );
}
