import 'package:bike_tracker/models/ride.dart';
import 'package:bike_tracker/models/track_point.dart';
import 'package:bike_tracker/utils/formatters.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('Ride & TrackPoint Model Tests', () {
    test('Ride model parses map data correctly', () {
      final now = DateTime.now();
      final data = {
        'userId': 'usr-123',
        'title': 'Evening Commute',
        'startTime': now.toIso8601String(),
        'endTime': now.add(const Duration(minutes: 45)).toIso8601String(),
        'distanceKm': 15.5,
        'durationSeconds': 2700,
        'avgSpeedKmh': 20.6,
        'maxSpeedKmh': 34.2,
        'elevationGain': 85.0,
        'encodedPolyline': 'some_polyline_str',
      };

      final ride = Ride.fromMap('doc-1', data);

      expect(ride.id, equals('doc-1'));
      expect(ride.userId, equals('usr-123'));
      expect(ride.title, equals('Evening Commute'));
      expect(ride.distanceKm, equals(15.5));
      expect(ride.duration.inMinutes, equals(45));
      expect(ride.avgSpeedKmh, equals(20.6));
      expect(ride.elevationGain, equals(85.0));
      expect(ride.encodedPolyline, equals('some_polyline_str'));
    });

    test('TrackPoint toMap and fromMap serialization roundtrip', () {
      final now = DateTime.now();
      final point = TrackPoint(
        latitude: 52.2297,
        longitude: 21.0122,
        elevation: 110.5,
        timestamp: now,
        speed: 6.0,
        accuracy: 4.2,
      );

      final map = point.toMap();
      final restored = TrackPoint.fromMap(map);

      expect(restored.latitude, equals(52.2297));
      expect(restored.longitude, equals(21.0122));
      expect(restored.elevation, equals(110.5));
      expect(restored.speed, equals(6.0));
      expect(restored.accuracy, equals(4.2));
    });

    test('Formatters format distances, durations and speeds correctly', () {
      expect(Formatters.formatDistance(0.85), equals('850 meters'));
      expect(Formatters.formatDistance(12.456), equals('12.46 km'));
      expect(Formatters.formatDuration(const Duration(hours: 1, minutes: 23, seconds: 45)), equals('1:23:45'));
      expect(Formatters.formatDuration(const Duration(minutes: 5, seconds: 9)), equals('05:09'));
      expect(Formatters.formatSpeed(24.56), equals('24.6 km/h'));
      expect(Formatters.formatElevation(124.8), equals('125 m'));
      expect(Formatters.formatElevation(null), equals('-- m'));
    });
  });
}
