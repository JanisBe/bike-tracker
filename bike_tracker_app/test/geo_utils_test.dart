import 'package:bike_tracker/models/track_point.dart';
import 'package:bike_tracker/utils/geo_utils.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('GeoUtils Tests', () {
    test('Haversine distance calculation is accurate within expected bounds', () {
      // Warsaw Palace of Culture (52.2319, 21.0067) to Old Town Market (52.2497, 21.0122)
      // Distance is approximately 2.0 km (straight-line ~2000 meters)
      final distance = GeoUtils.distanceMeters(
        52.2319,
        21.0067,
        52.2497,
        21.0122,
      );

      expect(distance, greaterThan(1900));
      expect(distance, lessThan(2100));
    });

    test('calculateTotalDistanceKm computes cumulative distance across points', () {
      final points = [
        TrackPoint(
          latitude: 52.2319,
          longitude: 21.0067,
          timestamp: DateTime(2026, 9, 16, 10, 0, 0),
        ),
        TrackPoint(
          latitude: 52.2400,
          longitude: 21.0090,
          timestamp: DateTime(2026, 9, 16, 10, 5, 0),
        ),
        TrackPoint(
          latitude: 52.2497,
          longitude: 21.0122,
          timestamp: DateTime(2026, 9, 16, 10, 10, 0),
        ),
      ];

      final totalKm = GeoUtils.calculateTotalDistanceKm(points);
      expect(totalKm, greaterThan(1.9));
      expect(totalKm, lessThan(2.2));
    });

    test('Google Polyline encode and decode roundtrip maintains precision', () {
      final points = [
        TrackPoint(
          latitude: 52.23190,
          longitude: 21.00670,
          timestamp: DateTime.now(),
        ),
        TrackPoint(
          latitude: 52.24000,
          longitude: 21.00900,
          timestamp: DateTime.now(),
        ),
        TrackPoint(
          latitude: 52.24970,
          longitude: 21.01220,
          timestamp: DateTime.now(),
        ),
      ];

      final encoded = GeoUtils.encodePolyline(points);
      expect(encoded, isNotEmpty);

      final decoded = GeoUtils.decodePolyline(encoded);
      expect(decoded.length, equals(3));

      // Precision up to 5 decimal places (~1 meter)
      expect(decoded[0].latitude, closeTo(52.23190, 0.00002));
      expect(decoded[0].longitude, closeTo(21.00670, 0.00002));
      expect(decoded[2].latitude, closeTo(52.24970, 0.00002));
      expect(decoded[2].longitude, closeTo(21.01220, 0.00002));
    });

    test('calculateElevationGain ignores noise fluctuations below threshold', () {
      final points = [
        TrackPoint(latitude: 0, longitude: 0, elevation: 100.0, timestamp: DateTime.now()),
        TrackPoint(latitude: 0, longitude: 0, elevation: 100.5, timestamp: DateTime.now()), // +0.5m ignored
        TrackPoint(latitude: 0, longitude: 0, elevation: 105.0, timestamp: DateTime.now()), // +5m gained
        TrackPoint(latitude: 0, longitude: 0, elevation: 104.0, timestamp: DateTime.now()), // -1m
        TrackPoint(latitude: 0, longitude: 0, elevation: 110.0, timestamp: DateTime.now()), // +6m gained
      ];

      final gain = GeoUtils.calculateElevationGain(points, thresholdMeters: 1.5);
      // Gained from 100.0 to 105.0 (5m), dip to 104.0 ignored (within 1.5m noise), then to 110.0 (+5m from 105.0) -> total 10.0m
      expect(gain, equals(10.0));
    });
  });
}
