import 'package:intl/intl.dart';

class Formatters {
  static final DateFormat _dateFormat = DateFormat('yyyy-MM-dd HH:mm');
  static final DateFormat _dateOnlyFormat = DateFormat('d MMM yyyy');
  static final DateFormat _timeOnlyFormat = DateFormat('HH:mm');

  /// Formats distance in km (e.g. "12.4 km") or meters if under 1km ("850 m")
  static String formatDistance(double km, {bool short = false}) {
    if (km < 1.0) {
      final meters = (km * 1000).round();
      return '$meters ${short ? 'm' : 'meters'}';
    }
    return '${km.toStringAsFixed(2)} ${short ? 'km' : 'km'}';
  }

  /// Formats duration into "HH:mm:ss" or "mm:ss"
  static String formatDuration(Duration duration) {
    final hours = duration.inHours;
    final minutes = duration.inMinutes.remainder(60).toString().padLeft(2, '0');
    final seconds = duration.inSeconds.remainder(60).toString().padLeft(2, '0');
    if (hours > 0) {
      return '$hours:$minutes:$seconds';
    }
    return '$minutes:$seconds';
  }

  /// Formats speed into "24.5 km/h"
  static String formatSpeed(double kmh) {
    return '${kmh.toStringAsFixed(1)} km/h';
  }

  /// Formats elevation in meters
  static String formatElevation(double? meters) {
    if (meters == null) return '-- m';
    return '${meters.round()} m';
  }

  /// Formats full DateTime
  static String formatDateTime(DateTime dt) {
    return _dateFormat.format(dt);
  }

  /// Formats friendly date and time
  static String formatFriendlyDate(DateTime dt) {
    return '${_dateOnlyFormat.format(dt)} at ${_timeOnlyFormat.format(dt)}';
  }
}
