import 'package:flutter/material.dart';

class AppConstants {
  static const String appName = 'Bike Tracker';

  // OpenStreetMap tile template
  static const String mapTileUrl = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
  static const String mapUserAgentPackage = 'com.biketracker.app';

  // GPS Configuration
  static const double maxAcceptableAccuracyMeters = 25.0;
  static const int minDistanceFilterMeters = 5; // record point if moved at least 5m
  static const Duration locationInterval = Duration(seconds: 3);

  // Styling & Theme Colors (Electric Teal & Dark Slate)
  static const Color primaryColor = Color(0xFF00E676); // Vibrant cycling neon green / teal
  static const Color primaryDark = Color(0xFF00B0FF); // Accent cyan
  static const Color accentOrange = Color(0xFFFF9100);
  static const Color darkBg = Color(0xFF121820);
  static const Color cardBg = Color(0xFF1C2430);
  static const Color cardBorder = Color(0xFF2B3747);
  static const Color textPrimary = Color(0xFFF1F5F9);
  static const Color textMuted = Color(0xFF94A3B8);
  static const Color polylineColor = Color(0xFF00E676);
}
