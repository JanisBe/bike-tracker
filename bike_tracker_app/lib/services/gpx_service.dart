import 'dart:io';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';
import 'package:xml/xml.dart';
import '../models/track_point.dart';

class GpxService {
  /// Generates a compliant GPX 1.1 XML string from a list of [TrackPoint]s.
  String generateGpxString(
    List<TrackPoint> points, {
    String trackName = 'Bike Ride',
    DateTime? startTime,
  }) {
    final builder = XmlBuilder();
    builder.processing('xml', 'version="1.0" encoding="UTF-8"');

    final metaTime = (startTime ?? (points.isNotEmpty ? points.first.timestamp : DateTime.now()))
        .toUtc()
        .toIso8601String();

    builder.element('gpx', attributes: {
      'version': '1.1',
      'creator': 'BikeTracker',
      'xmlns': 'http://www.topografix.com/GPX/1/1',
      'xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance',
      'xsi:schemaLocation':
          'http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd',
    }, nest: () {
      // Metadata
      builder.element('metadata', nest: () {
        builder.element('name', nest: trackName);
        builder.element('time', nest: metaTime);
      });

      // Track
      builder.element('trk', nest: () {
        builder.element('name', nest: trackName);
        builder.element('trkseg', nest: () {
          for (final pt in points) {
            builder.element('trkpt', attributes: {
              'lat': pt.latitude.toStringAsFixed(6),
              'lon': pt.longitude.toStringAsFixed(6),
            }, nest: () {
              if (pt.elevation != null) {
                builder.element('ele', nest: pt.elevation!.toStringAsFixed(1));
              }
              builder.element('time', nest: pt.timestamp.toUtc().toIso8601String());
              if (pt.speed != null && pt.speed! >= 0) {
                // Extensions for speed if needed
                builder.element('speed', nest: pt.speed!.toStringAsFixed(2));
              }
            });
          }
        });
      });
    });

    final document = builder.buildDocument();
    return document.toXmlString(pretty: true, indent: '  ');
  }

  /// Writes GPX XML string to a file in application documents or temp directory.
  Future<File> saveGpxToTempFile(String gpxContent, String fileName) async {
    final tempDir = await getTemporaryDirectory();
    final cleanFileName = fileName.endsWith('.gpx') ? fileName : '$fileName.gpx';
    final file = File('${tempDir.path}/$cleanFileName');
    return await file.writeAsString(gpxContent);
  }

  /// Exports / Shares GPX file using system share sheet.
  Future<void> shareGpx(String gpxContent, String fileName) async {
    final file = await saveGpxToTempFile(gpxContent, fileName);
    await SharePlus.instance.share(
      ShareParams(
        files: [XFile(file.path, mimeType: 'application/gpx+xml')],
        subject: fileName,
      ),
    );
  }
}
