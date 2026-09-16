class TrackPoint {
  final double latitude;
  final double longitude;
  final double? elevation;
  final DateTime timestamp;
  final double? speed; // in m/s
  final double? accuracy; // in meters

  const TrackPoint({
    required this.latitude,
    required this.longitude,
    this.elevation,
    required this.timestamp,
    this.speed,
    this.accuracy,
  });

  Map<String, dynamic> toMap() {
    return {
      'latitude': latitude,
      'longitude': longitude,
      'elevation': elevation,
      'timestamp': timestamp.toIso8601String(),
      'speed': speed,
      'accuracy': accuracy,
    };
  }

  factory TrackPoint.fromMap(Map<String, dynamic> map) {
    return TrackPoint(
      latitude: (map['latitude'] as num).toDouble(),
      longitude: (map['longitude'] as num).toDouble(),
      elevation: (map['elevation'] as num?)?.toDouble(),
      timestamp: DateTime.parse(map['timestamp'] as String),
      speed: (map['speed'] as num?)?.toDouble(),
      accuracy: (map['accuracy'] as num?)?.toDouble(),
    );
  }
}
