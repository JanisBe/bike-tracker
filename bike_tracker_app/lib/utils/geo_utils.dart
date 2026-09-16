import 'dart:math' as math;
import 'package:latlong2/latlong.dart';
import '../models/track_point.dart';

class GeoUtils {
  static const double earthRadiusMeters = 6371000.0;

  /// Calculates the Haversine distance in meters between two coordinates.
  static double distanceMeters(
    double lat1,
    double lon1,
    double lat2,
    double lon2,
  ) {
    final dLat = _deg2rad(lat2 - lat1);
    final dLon = _deg2rad(lon2 - lon1);

    final a = math.sin(dLat / 2) * math.sin(dLat / 2) +
        math.cos(_deg2rad(lat1)) *
            math.cos(_deg2rad(lat2)) *
            math.sin(dLon / 2) *
            math.sin(dLon / 2);

    final c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a));
    return earthRadiusMeters * c;
  }

  /// Calculates the total distance in kilometers for a list of track points.
  static double calculateTotalDistanceKm(List<TrackPoint> points) {
    if (points.length < 2) return 0.0;
    double totalMeters = 0.0;
    for (int i = 0; i < points.length - 1; i++) {
      totalMeters += distanceMeters(
        points[i].latitude,
        points[i].longitude,
        points[i + 1].latitude,
        points[i + 1].longitude,
      );
    }
    return totalMeters / 1000.0;
  }

  /// Calculates cumulative elevation gain in meters, ignoring micro-fluctuations under [thresholdMeters].
  static double calculateElevationGain(
    List<TrackPoint> points, {
    double thresholdMeters = 1.5,
  }) {
    if (points.length < 2) return 0.0;
    double gain = 0.0;
    double? lastValidEle;

    for (final pt in points) {
      if (pt.elevation == null) continue;
      if (lastValidEle == null) {
        lastValidEle = pt.elevation;
        continue;
      }
      final diff = pt.elevation! - lastValidEle;
      if (diff >= thresholdMeters) {
        gain += diff;
        lastValidEle = pt.elevation;
      } else if (diff < -thresholdMeters) {
        lastValidEle = pt.elevation;
      }
    }
    return gain;
  }

  /// Encodes a list of TrackPoints or LatLng into a Google Encoded Polyline string.
  static String encodePolyline(List<TrackPoint> points) {
    if (points.isEmpty) return '';
    final buffer = StringBuffer();
    int lastLat = 0;
    int lastLng = 0;

    for (final point in points) {
      final lat = (point.latitude * 1e5).round();
      final lng = (point.longitude * 1e5).round();

      final dLat = lat - lastLat;
      final dLng = lng - lastLng;

      _encodeChunk(dLat, buffer);
      _encodeChunk(dLng, buffer);

      lastLat = lat;
      lastLng = lng;
    }

    return buffer.toString();
  }

  /// Decodes an Encoded Polyline string into a list of [LatLng] for flutter_map.
  static List<LatLng> decodePolyline(String encoded) {
    final List<LatLng> points = [];
    if (encoded.isEmpty) return points;

    int index = 0;
    int lat = 0;
    int lng = 0;

    while (index < encoded.length) {
      int shift = 0;
      int result = 0;
      int b;
      do {
        b = encoded.codeUnitAt(index++) - 63;
        result |= (b & 0x1f) << shift;
        shift += 5;
      } while (b >= 0x20);
      final dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
      lat += dlat;

      shift = 0;
      result = 0;
      do {
        b = encoded.codeUnitAt(index++) - 63;
        result |= (b & 0x1f) << shift;
        shift += 5;
      } while (b >= 0x20);
      final dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
      lng += dlng;

      points.add(LatLng(lat / 1e5, lng / 1e5));
    }

    return points;
  }

  static void _encodeChunk(int value, StringBuffer buffer) {
    int v = value < 0 ? ~(value << 1) : (value << 1);
    while (v >= 0x20) {
      buffer.writeCharCode((0x20 | (v & 0x1f)) + 63);
      v >>= 5;
    }
    buffer.writeCharCode(v + 63);
  }

  static double _deg2rad(double deg) => deg * (math.pi / 180.0);
}
