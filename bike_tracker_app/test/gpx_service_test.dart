import 'package:bike_tracker/models/track_point.dart';
import 'package:bike_tracker/services/gpx_service.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:xml/xml.dart';

void main() {
  group('GpxService Tests', () {
    test('generateGpxString produces valid GPX 1.1 XML structure', () {
      final gpxService = GpxService();
      final now = DateTime.utc(2026, 9, 16, 12, 0, 0);

      final points = [
        TrackPoint(
          latitude: 52.2297,
          longitude: 21.0122,
          elevation: 105.4,
          timestamp: now,
          speed: 5.5, // ~19.8 km/h
        ),
        TrackPoint(
          latitude: 52.2350,
          longitude: 21.0180,
          elevation: 108.2,
          timestamp: now.add(const Duration(seconds: 30)),
          speed: 6.2,
        ),
      ];

      final gpxString = gpxService.generateGpxString(
        points,
        trackName: 'Warsaw Scenic Ride',
        startTime: now,
      );

      expect(gpxString, contains('<?xml version="1.0" encoding="UTF-8"?>'));
      expect(gpxString, contains('<gpx version="1.1" creator="BikeTracker"'));
      expect(gpxString, contains('<name>Warsaw Scenic Ride</name>'));
      expect(gpxString, contains('<trkpt lat="52.229700" lon="21.012200">'));
      expect(gpxString, contains('<ele>105.4</ele>'));
      expect(gpxString, contains('<time>2026-09-16T12:00:00.000Z</time>'));

      // Validate XML can be parsed by XmlDocument without throwing
      final parsedDoc = XmlDocument.parse(gpxString);
      final trkpts = parsedDoc.findAllElements('trkpt');
      expect(trkpts.length, equals(2));
    });
  });
}
