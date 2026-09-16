import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter/foundation.dart';
import '../models/ride.dart';
import '../models/track_point.dart';
import '../utils/geo_utils.dart';
import 'gpx_service.dart';

class RideService {
  final FirebaseFirestore _firestore = FirebaseFirestore.instance;
  final GpxService _gpxService = GpxService();

  CollectionReference get _ridesCollection => _firestore.collection('rides');

  /// Saves a recorded bike ride with metadata and GPX content in a single batch.
  Future<Ride> saveRide({
    required String userId,
    required List<TrackPoint> points,
    String? title,
  }) async {
    if (points.isEmpty) {
      throw ArgumentError('Cannot save ride with 0 track points');
    }

    final startTime = points.first.timestamp;
    final endTime = points.last.timestamp;
    final durationSeconds = endTime.difference(startTime).inSeconds;
    final distanceKm = GeoUtils.calculateTotalDistanceKm(points);
    final elevationGain = GeoUtils.calculateElevationGain(points);
    final encodedPolyline = GeoUtils.encodePolyline(points);

    // Calculate speeds
    final avgSpeedKmh = durationSeconds > 0
        ? (distanceKm / (durationSeconds / 3600.0))
        : 0.0;

    double maxSpeedKmh = 0.0;
    for (final pt in points) {
      if (pt.speed != null) {
        final speedKmh = pt.speed! * 3.6;
        if (speedKmh > maxSpeedKmh) maxSpeedKmh = speedKmh;
      }
    }
    if (maxSpeedKmh < avgSpeedKmh) {
      maxSpeedKmh = avgSpeedKmh * 1.2;
    }

    final rideTitle = title?.trim().isNotEmpty == true
        ? title!.trim()
        : 'Ride on ${startTime.year}-${startTime.month.toString().padLeft(2, '0')}-${startTime.day.toString().padLeft(2, '0')}';

    // Generate GPX string
    final gpxContent = _gpxService.generateGpxString(
      points,
      trackName: rideTitle,
      startTime: startTime,
    );

    // Firestore Batch Write
    final rideRef = _ridesCollection.doc();
    final gpxRef = rideRef.collection('details').doc('gpx');

    final rideData = {
      'userId': userId,
      'title': rideTitle,
      'startTime': Timestamp.fromDate(startTime),
      'endTime': Timestamp.fromDate(endTime),
      'distanceKm': double.parse(distanceKm.toStringAsFixed(2)),
      'durationSeconds': durationSeconds,
      'avgSpeedKmh': double.parse(avgSpeedKmh.toStringAsFixed(1)),
      'maxSpeedKmh': double.parse(maxSpeedKmh.toStringAsFixed(1)),
      'elevationGain': double.parse(elevationGain.toStringAsFixed(1)),
      'encodedPolyline': encodedPolyline,
      'createdAt': FieldValue.serverTimestamp(),
    };

    final batch = _firestore.batch();
    batch.set(rideRef, rideData);
    batch.set(gpxRef, {
      'userId': userId,
      'gpxContent': gpxContent,
    });

    await batch.commit();

    return Ride(
      id: rideRef.id,
      userId: userId,
      title: rideTitle,
      startTime: startTime,
      endTime: endTime,
      distanceKm: distanceKm,
      durationSeconds: durationSeconds,
      avgSpeedKmh: avgSpeedKmh,
      maxSpeedKmh: maxSpeedKmh,
      elevationGain: elevationGain,
      encodedPolyline: encodedPolyline,
      createdAt: DateTime.now(),
    );
  }

  /// Streams rides for the logged-in user ordered by startTime descending.
  Stream<List<Ride>> streamUserRides(String userId) {
    return _ridesCollection
        .where('userId', isEqualTo: userId)
        .orderBy('startTime', descending: true)
        .snapshots()
        .map((snapshot) {
      return snapshot.docs.map((doc) => Ride.fromFirestore(doc)).toList();
    });
  }

  /// Fetches GPX XML content from the subcollection `rides/{rideId}/details/gpx`.
  Future<String?> fetchRideGpx(String rideId) async {
    try {
      final gpxDoc = await _ridesCollection
          .doc(rideId)
          .collection('details')
          .doc('gpx')
          .get();

      if (gpxDoc.exists && gpxDoc.data() != null) {
        return gpxDoc.data()!['gpxContent'] as String?;
      }
      return null;
    } catch (e) {
      debugPrint('Error fetching GPX: $e');
      return null;
    }
  }

  /// Deletes ride metadata and GPX subcollection document.
  Future<void> deleteRide(String rideId) async {
    final batch = _firestore.batch();
    final rideRef = _ridesCollection.doc(rideId);
    final gpxRef = rideRef.collection('details').doc('gpx');

    batch.delete(gpxRef);
    batch.delete(rideRef);

    await batch.commit();
  }
}
