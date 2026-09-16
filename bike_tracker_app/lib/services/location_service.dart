import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:geolocator/geolocator.dart';
import '../models/track_point.dart';
import '../utils/constants.dart';
import '../utils/geo_utils.dart';

class LocationService extends ChangeNotifier {
  StreamSubscription<Position>? _positionSubscription;
  Timer? _tickerTimer;

  bool _isTracking = false;
  bool _isPaused = false;
  DateTime? _trackingStartTime;
  DateTime? _lastResumeTime;
  Duration _accumulatedDuration = Duration.zero;

  final List<TrackPoint> _currentTrack = [];
  Position? _currentPosition;
  double _currentDistanceKm = 0.0;
  double _currentSpeedKmh = 0.0;
  double _maxSpeedKmh = 0.0;

  // Getters
  bool get isTracking => _isTracking;
  bool get isPaused => _isPaused;
  List<TrackPoint> get currentTrack => List.unmodifiable(_currentTrack);
  Position? get currentPosition => _currentPosition;
  double get currentDistanceKm => _currentDistanceKm;
  double get currentSpeedKmh => _currentSpeedKmh;
  double get maxSpeedKmh => _maxSpeedKmh;

  Duration get elapsedDuration {
    if (!_isTracking) return Duration.zero;
    if (_isPaused || _lastResumeTime == null) return _accumulatedDuration;
    return _accumulatedDuration + DateTime.now().difference(_lastResumeTime!);
  }

  /// Verifies location services and permissions.
  Future<bool> checkAndRequestPermissions() async {
    bool serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      return false;
    }

    LocationPermission permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
      if (permission == LocationPermission.denied) {
        return false;
      }
    }

    if (permission == LocationPermission.deniedForever) {
      return false;
    }

    return true;
  }

  /// Starts recording GPS coordinates.
  Future<bool> startTracking() async {
    final hasPermission = await checkAndRequestPermissions();
    if (!hasPermission) return false;

    // Reset state
    _currentTrack.clear();
    _currentDistanceKm = 0.0;
    _currentSpeedKmh = 0.0;
    _maxSpeedKmh = 0.0;
    _accumulatedDuration = Duration.zero;
    _trackingStartTime = DateTime.now();
    _lastResumeTime = _trackingStartTime;
    _isTracking = true;
    _isPaused = false;

    _startDurationTicker();
    _startLocationUpdates();

    notifyListeners();
    return true;
  }

  /// Pauses the current tracking session.
  void pauseTracking() {
    if (!_isTracking || _isPaused) return;

    if (_lastResumeTime != null) {
      _accumulatedDuration += DateTime.now().difference(_lastResumeTime!);
      _lastResumeTime = null;
    }
    _isPaused = true;
    _currentSpeedKmh = 0.0;
    _positionSubscription?.pause();
    notifyListeners();
  }

  /// Resumes the paused tracking session.
  void resumeTracking() {
    if (!_isTracking || !_isPaused) return;

    _lastResumeTime = DateTime.now();
    _isPaused = false;
    _positionSubscription?.resume();
    notifyListeners();
  }

  /// Stops tracking and returns the collected points.
  List<TrackPoint> stopTracking() {
    if (_lastResumeTime != null) {
      _accumulatedDuration += DateTime.now().difference(_lastResumeTime!);
      _lastResumeTime = null;
    }

    _isTracking = false;
    _isPaused = false;
    _currentSpeedKmh = 0.0;

    _stopDurationTicker();
    _positionSubscription?.cancel();
    _positionSubscription = null;

    final recordedPoints = List<TrackPoint>.from(_currentTrack);
    notifyListeners();
    return recordedPoints;
  }

  void _startDurationTicker() {
    _tickerTimer?.cancel();
    _tickerTimer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (_isTracking && !_isPaused) {
        notifyListeners();
      }
    });
  }

  void _stopDurationTicker() {
    _tickerTimer?.cancel();
    _tickerTimer = null;
  }

  void _startLocationUpdates() {
    late final LocationSettings locationSettings;

    if (defaultTargetPlatform == TargetPlatform.android) {
      locationSettings = AndroidSettings(
        accuracy: LocationAccuracy.high,
        distanceFilter: AppConstants.minDistanceFilterMeters,
        intervalDuration: AppConstants.locationInterval,
        foregroundNotificationConfig: const ForegroundNotificationConfig(
          notificationTitle: "Bike Tracker",
          notificationText: "Tracking your ride in background...",
          enableWakeLock: true,
        ),
      );
    } else if (defaultTargetPlatform == TargetPlatform.iOS ||
        defaultTargetPlatform == TargetPlatform.macOS) {
      locationSettings = AppleSettings(
        accuracy: LocationAccuracy.high,
        activityType: ActivityType.fitness,
        distanceFilter: AppConstants.minDistanceFilterMeters,
        pauseLocationUpdatesAutomatically: false,
        showBackgroundLocationIndicator: true,
      );
    } else {
      locationSettings = const LocationSettings(
        accuracy: LocationAccuracy.high,
        distanceFilter: AppConstants.minDistanceFilterMeters,
      );
    }

    _positionSubscription?.cancel();
    _positionSubscription = Geolocator.getPositionStream(
      locationSettings: locationSettings,
    ).listen(_handleNewPosition, onError: (e) {
      debugPrint('Location stream error: $e');
    });
  }

  void _handleNewPosition(Position position) {
    if (!_isTracking || _isPaused) return;

    // Filter points with low accuracy
    if (position.accuracy > AppConstants.maxAcceptableAccuracyMeters) {
      return;
    }

    final now = DateTime.now();
    final point = TrackPoint(
      latitude: position.latitude,
      longitude: position.longitude,
      elevation: position.altitude,
      timestamp: now,
      speed: position.speed,
      accuracy: position.accuracy,
    );

    // Calculate distance and speeds
    if (_currentTrack.isNotEmpty) {
      final lastPoint = _currentTrack.last;
      final addedDistanceMeters = GeoUtils.distanceMeters(
        lastPoint.latitude,
        lastPoint.longitude,
        point.latitude,
        point.longitude,
      );
      _currentDistanceKm += (addedDistanceMeters / 1000.0);
    }

    // Speed conversion: m/s to km/h
    final speedKmh = position.speed >= 0 ? (position.speed * 3.6) : 0.0;
    _currentSpeedKmh = speedKmh;
    if (speedKmh > _maxSpeedKmh) {
      _maxSpeedKmh = speedKmh;
    }

    _currentPosition = position;
    _currentTrack.add(point);
    notifyListeners();
  }

  @override
  void dispose() {
    _stopDurationTicker();
    _positionSubscription?.cancel();
    super.dispose();
  }
}
