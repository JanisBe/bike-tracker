import 'package:cloud_firestore/cloud_firestore.dart';

class Ride {
  final String id;
  final String userId;
  final String title;
  final DateTime startTime;
  final DateTime endTime;
  final double distanceKm;
  final int durationSeconds;
  final double avgSpeedKmh;
  final double maxSpeedKmh;
  final double? elevationGain;
  final String encodedPolyline;
  final DateTime? createdAt;

  const Ride({
    required this.id,
    required this.userId,
    required this.title,
    required this.startTime,
    required this.endTime,
    required this.distanceKm,
    required this.durationSeconds,
    required this.avgSpeedKmh,
    required this.maxSpeedKmh,
    this.elevationGain,
    required this.encodedPolyline,
    this.createdAt,
  });

  Duration get duration => Duration(seconds: durationSeconds);

  factory Ride.fromFirestore(DocumentSnapshot doc) {
    final data = doc.data() as Map<String, dynamic>? ?? {};
    return Ride.fromMap(doc.id, data);
  }

  factory Ride.fromMap(String id, Map<String, dynamic> data) {
    DateTime parseDateTime(dynamic value) {
      if (value is Timestamp) return value.toDate();
      if (value is String) return DateTime.tryParse(value) ?? DateTime.now();
      if (value is int) return DateTime.fromMillisecondsSinceEpoch(value);
      return DateTime.now();
    }

    return Ride(
      id: id,
      userId: (data['userId'] as String?) ?? '',
      title: (data['title'] as String?) ?? 'Bike Ride',
      startTime: parseDateTime(data['startTime']),
      endTime: parseDateTime(data['endTime']),
      distanceKm: (data['distanceKm'] as num?)?.toDouble() ?? 0.0,
      durationSeconds: (data['durationSeconds'] as num?)?.toInt() ?? 0,
      avgSpeedKmh: (data['avgSpeedKmh'] as num?)?.toDouble() ?? 0.0,
      maxSpeedKmh: (data['maxSpeedKmh'] as num?)?.toDouble() ?? 0.0,
      elevationGain: (data['elevationGain'] as num?)?.toDouble(),
      encodedPolyline: (data['encodedPolyline'] as String?) ?? '',
      createdAt: data['createdAt'] != null ? parseDateTime(data['createdAt']) : null,
    );
  }

  Map<String, dynamic> toFirestore() {
    return {
      'userId': userId,
      'title': title,
      'startTime': Timestamp.fromDate(startTime),
      'endTime': Timestamp.fromDate(endTime),
      'distanceKm': distanceKm,
      'durationSeconds': durationSeconds,
      'avgSpeedKmh': avgSpeedKmh,
      'maxSpeedKmh': maxSpeedKmh,
      'elevationGain': elevationGain,
      'encodedPolyline': encodedPolyline,
      'createdAt': createdAt != null ? Timestamp.fromDate(createdAt!) : FieldValue.serverTimestamp(),
    };
  }
}
