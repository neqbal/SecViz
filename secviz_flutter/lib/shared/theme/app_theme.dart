import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class AppColors {
  // Backgrounds
  static const bgPrimary = Color(0xFF0D1117);
  static const bgSurface = Color(0xFF161B22);
  static const bgElevated = Color(0xFF21262D);
  static const bgCard = Color(0xFF1C2128);

  // Borders
  static const border = Color(0xFF30363D);

  // Accents
  static const accent = Color(0xFF58A6FF);
  static const success = Color(0xFF3FB950);
  static const danger = Color(0xFFFF7B72);
  static const warning = Color(0xFFD29922);
  static const purple = Color(0xFFA371F7);
  static const pink = Color(0xFFF778BA);

  // Text
  static const textPrimary = Color(0xFFE6EDF3);
  static const textSecondary = Color(0xFF8B949E);
  static const textMuted = Color(0xFF484F58);

  // Stack block colors
  static const blockMainFrame = Color(0xFF1E1128);
  static const blockSafe = Color(0xFF0D2518);
  static const blockWarn = Color(0xFF1F1500);
  static const blockDanger = Color(0xFF1F0D0D);
  static const blockFilled = Color(0xFF0D1F2E);
  static const blockNeutral = Color(0xFF161B22);
  static const blockJunk = Color(0xFF3D1212);
  static const blockTarget = Color(0xFF0D2038);

  // Register colors
  static const regRip = Color(0xFFFF7B72);
  static const regRsp = Color(0xFF79C0FF);
  static const regRbp = Color(0xFFD2A8FF);
  static const regRax = Color(0xFF56D364);
  static const regOther = Color(0xFF8B949E);
}

class AppTheme {
  static ThemeData get dark {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      scaffoldBackgroundColor: AppColors.bgPrimary,
      colorScheme: const ColorScheme.dark(
        primary: AppColors.accent,
        secondary: AppColors.purple,
        error: AppColors.danger,
        surface: AppColors.bgSurface,
        onPrimary: AppColors.textPrimary,
        onSurface: AppColors.textPrimary,
      ),
      textTheme: GoogleFonts.interTextTheme(ThemeData.dark().textTheme).copyWith(
        bodyMedium: GoogleFonts.inter(color: AppColors.textPrimary),
        bodySmall: GoogleFonts.inter(color: AppColors.textSecondary),
        titleMedium: GoogleFonts.inter(
            color: AppColors.textPrimary, fontWeight: FontWeight.w600),
        titleLarge: GoogleFonts.inter(
            color: AppColors.textPrimary, fontWeight: FontWeight.w700),
      ),
      cardTheme: CardThemeData(
        color: AppColors.bgCard,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
          side: const BorderSide(color: AppColors.border),
        ),
      ),
      appBarTheme: AppBarTheme(
        backgroundColor: AppColors.bgSurface,
        foregroundColor: AppColors.textPrimary,
        elevation: 0,
        centerTitle: false,
        titleTextStyle: GoogleFonts.inter(
          color: AppColors.textPrimary,
          fontSize: 16,
          fontWeight: FontWeight.w600,
        ),
        surfaceTintColor: Colors.transparent,
      ),
      dividerTheme: const DividerThemeData(
        color: AppColors.border,
        thickness: 1,
        space: 1,
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: AppColors.bgElevated,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8),
          borderSide: const BorderSide(color: AppColors.border),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8),
          borderSide: const BorderSide(color: AppColors.border),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8),
          borderSide: const BorderSide(color: AppColors.accent, width: 2),
        ),
        hintStyle: const TextStyle(color: AppColors.textMuted),
        contentPadding:
            const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.accent,
          foregroundColor: AppColors.bgPrimary,
          textStyle: GoogleFonts.inter(fontWeight: FontWeight.w600),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(8),
          ),
        ),
      ),
      chipTheme: ChipThemeData(
        backgroundColor: AppColors.bgElevated,
        labelStyle: const TextStyle(color: AppColors.textSecondary),
        side: const BorderSide(color: AppColors.border),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(6)),
      ),
      bottomSheetTheme: const BottomSheetThemeData(
        backgroundColor: AppColors.bgSurface,
        modalBackgroundColor: AppColors.bgSurface,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
        ),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: AppColors.bgSurface,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      ),
      snackBarTheme: const SnackBarThemeData(
        backgroundColor: AppColors.bgElevated,
        contentTextStyle: TextStyle(color: AppColors.textPrimary),
      ),
    );
  }
}
