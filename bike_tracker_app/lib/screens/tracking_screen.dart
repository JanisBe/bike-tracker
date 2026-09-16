import 'package:flutter/material.dart';
import 'package:latlong2/latlong.dart';
import 'package:provider/provider.dart';
import '../services/auth_service.dart';
import '../services/location_service.dart';
import '../services/ride_service.dart';
import '../utils/constants.dart';
import '../utils/formatters.dart';
import '../widgets/live_map.dart';
import '../widgets/stats_bar.dart';
import 'ride_detail_screen.dart';

class TrackingScreen extends StatefulWidget {
  const TrackingScreen({super.key});

  @override
  State<TrackingScreen> createState() => _TrackingScreenState();
}

class _TrackingScreenState extends State<TrackingScreen> {
  bool _isSaving = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final locService = context.read<LocationService>();
      if (!locService.isTracking) {
        locService.startTracking();
      }
    });
  }

  Future<void> _handleStopRide() async {
    final locService = context.read<LocationService>();
    locService.pauseTracking();

    final titleController = TextEditingController(
      text: 'Ride on ${DateTime.now().toLocal().toString().split(' ')[0]}',
    );

    final shouldSave = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppConstants.darkBg,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) => Padding(
        padding: EdgeInsets.only(
          bottom: MediaQuery.of(ctx).viewInsets.bottom + 20,
          left: 20,
          right: 20,
          top: 20,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Center(
              child: Container(
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                  color: AppConstants.cardBorder,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            const SizedBox(height: 16),
            const Text(
              'Finish Ride',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
                color: AppConstants.textPrimary,
              ),
            ),
            const SizedBox(height: 6),
            const Text(
              'Review your stats and name your ride',
              style: TextStyle(fontSize: 13, color: AppConstants.textMuted),
            ),
            const SizedBox(height: 16),

            // Mini stats preview
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: AppConstants.cardBg,
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: AppConstants.cardBorder),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceAround,
                children: [
                  _miniStat('DISTANCE', Formatters.formatDistance(locService.currentDistanceKm)),
                  _miniStat('DURATION', Formatters.formatDuration(locService.elapsedDuration)),
                  _miniStat('PTS', '${locService.currentTrack.length}'),
                ],
              ),
            ),
            const SizedBox(height: 16),

            if (locService.currentTrack.length < 2) ...[
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.amber.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: Colors.amber.withValues(alpha: 0.5)),
                ),
                child: const Row(
                  children: [
                    Icon(Icons.info_outline, color: Colors.amber, size: 20),
                    SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        'Ride too short to save (need at least 2 GPS points). You can finish without saving or continue riding.',
                        style: TextStyle(color: Colors.amber, fontSize: 13),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: () {
                        Navigator.pop(ctx, null); // Continue Riding
                      },
                      style: OutlinedButton.styleFrom(
                        foregroundColor: AppConstants.textPrimary,
                        side: const BorderSide(color: AppConstants.cardBorder),
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                      child: const Text('Continue Riding'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: ElevatedButton(
                      onPressed: () {
                        Navigator.pop(ctx, false); // Finish without saving
                      },
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.redAccent,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                      child: const Text(
                        'Finish without Saving',
                        style: TextStyle(fontWeight: FontWeight.bold),
                        textAlign: TextAlign.center,
                      ),
                    ),
                  ),
                ],
              ),
            ] else ...[
              // Title input
              TextField(
                controller: titleController,
                autofocus: true,
                style: const TextStyle(color: AppConstants.textPrimary),
                decoration: InputDecoration(
                  labelText: 'Ride Title',
                  labelStyle: const TextStyle(color: AppConstants.textMuted),
                  filled: true,
                  fillColor: AppConstants.cardBg,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: const BorderSide(color: AppConstants.cardBorder),
                  ),
                  enabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: const BorderSide(color: AppConstants.cardBorder),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: const BorderSide(color: AppConstants.primaryColor),
                  ),
                ),
              ),
              const SizedBox(height: 20),

              // Buttons
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: () {
                        Navigator.pop(ctx, false); // Discard
                      },
                      style: OutlinedButton.styleFrom(
                        foregroundColor: Colors.redAccent,
                        side: const BorderSide(color: Colors.redAccent),
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                      child: const Text('Discard'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    flex: 2,
                    child: ElevatedButton(
                      onPressed: () => Navigator.pop(ctx, true), // Save
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppConstants.primaryColor,
                        foregroundColor: Colors.black,
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                      child: const Text(
                        'Save Ride',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Center(
                child: TextButton(
                  onPressed: () => Navigator.pop(ctx, null), // Continue Riding
                  child: const Text(
                    'Continue Riding',
                    style: TextStyle(color: AppConstants.textMuted),
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );

    if (shouldSave == true) {
      final points = locService.stopTracking();
      if (points.length < 2) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Not enough GPS points collected to save.')),
          );
          Navigator.pop(context);
        }
        return;
      }

      setState(() => _isSaving = true);
      if (!mounted) return;
      try {
        final authService = context.read<AuthService>();
        final rideService = context.read<RideService>();
        final user = authService.currentUser;
        if (user == null) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Please sign in to save your ride.')),
          );
          setState(() => _isSaving = false);
          return;
        }

        final savedRide = await rideService.saveRide(
          userId: user.uid,
          points: points,
          title: titleController.text,
        );

        if (mounted) {
          Navigator.pushReplacement(
            context,
            MaterialPageRoute(
              builder: (_) => RideDetailScreen(ride: savedRide),
            ),
          );
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Error saving ride: $e')),
          );
          setState(() => _isSaving = false);
        }
      }
    } else if (shouldSave == false) {
      locService.stopTracking();
      if (mounted) Navigator.pop(context);
    } else {
      // User dismissed bottom sheet without choice -> resume tracking
      locService.resumeTracking();
    }
  }

  Widget _miniStat(String label, String value) {
    return Column(
      children: [
        Text(
          label,
          style: const TextStyle(fontSize: 10, color: AppConstants.textMuted, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 4),
        Text(
          value,
          style: const TextStyle(fontSize: 14, color: AppConstants.textPrimary, fontWeight: FontWeight.bold),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    final locService = context.watch<LocationService>();

    final routeLatLngs = locService.currentTrack
        .map((pt) => LatLng(pt.latitude, pt.longitude))
        .toList();

    final currentLatLng = locService.currentPosition != null
        ? LatLng(locService.currentPosition!.latitude, locService.currentPosition!.longitude)
        : null;

    return PopScope(
      canPop: !locService.isTracking,
      onPopInvokedWithResult: (didPop, result) {
        if (!didPop) {
          _handleStopRide();
        }
      },
      child: Scaffold(
        backgroundColor: AppConstants.darkBg,
        body: Stack(
          children: [
            // Interactive OpenStreetMap
            Positioned.fill(
              child: LiveMap(
                routePoints: routeLatLngs,
                currentLocation: currentLatLng,
                isTracking: true,
                autoCenter: true,
              ),
            ),

            // Top Status Badge & Back Button
            SafeArea(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 10.0),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    // Close button
                    FloatingActionButton.small(
                      heroTag: 'tracking_back',
                      backgroundColor: AppConstants.darkBg.withValues(alpha: 0.9),
                      foregroundColor: AppConstants.textPrimary,
                      onPressed: () => _handleStopRide(),
                      child: const Icon(Icons.arrow_back),
                    ),

                    // Tracking Status Pill
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                      decoration: BoxDecoration(
                        color: AppConstants.darkBg.withValues(alpha: 0.92),
                        borderRadius: BorderRadius.circular(20),
                        border: Border.all(
                          color: locService.isPaused
                              ? AppConstants.accentOrange
                              : AppConstants.primaryColor,
                          width: 1.5,
                        ),
                        boxShadow: [
                          BoxShadow(
                            color: (locService.isPaused
                                    ? AppConstants.accentOrange
                                    : AppConstants.primaryColor)
                                .withValues(alpha: 0.25),
                            blurRadius: 10,
                          ),
                        ],
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Container(
                            width: 8,
                            height: 8,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              color: locService.isPaused
                                  ? AppConstants.accentOrange
                                  : AppConstants.primaryColor,
                            ),
                          ),
                          const SizedBox(width: 8),
                          Text(
                            locService.isPaused ? 'PAUSED' : 'TRACKING LIVE',
                            style: TextStyle(
                              fontSize: 12,
                              fontWeight: FontWeight.w800,
                              letterSpacing: 0.8,
                              color: locService.isPaused
                                  ? AppConstants.accentOrange
                                  : AppConstants.primaryColor,
                            ),
                          ),
                        ],
                      ),
                    ),

                    // GPS Accuracy Indicator
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                      decoration: BoxDecoration(
                        color: AppConstants.darkBg.withValues(alpha: 0.9),
                        borderRadius: BorderRadius.circular(20),
                        border: Border.all(color: AppConstants.cardBorder),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(Icons.gps_fixed, size: 14, color: AppConstants.primaryDark),
                          const SizedBox(width: 4),
                          Text(
                            locService.currentPosition != null
                                ? '±${locService.currentPosition!.accuracy.round()}m'
                                : 'Searching',
                            style: const TextStyle(fontSize: 11, color: AppConstants.textMuted),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),

            // Bottom HUD: StatsBar and Action Controls
            Positioned(
              left: 16,
              right: 16,
              bottom: 24,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  // Live Stats
                  StatsBar(
                    distanceKm: locService.currentDistanceKm,
                    duration: locService.elapsedDuration,
                    speedKmh: locService.currentSpeedKmh,
                    elevationMeters: locService.currentPosition?.altitude,
                  ),
                  const SizedBox(height: 14),

                  // Action Buttons (Pause/Resume & Stop)
                  Row(
                    children: [
                      // Pause / Resume
                      Expanded(
                        child: ElevatedButton.icon(
                          onPressed: () {
                            if (locService.isPaused) {
                              locService.resumeTracking();
                            } else {
                              locService.pauseTracking();
                            }
                          },
                          style: ElevatedButton.styleFrom(
                            backgroundColor: locService.isPaused
                                ? AppConstants.primaryColor
                                : AppConstants.accentOrange,
                            foregroundColor: Colors.black,
                            padding: const EdgeInsets.symmetric(vertical: 16),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(16),
                            ),
                            elevation: 4,
                          ),
                          icon: Icon(
                            locService.isPaused ? Icons.play_arrow : Icons.pause,
                            size: 22,
                          ),
                          label: Text(
                            locService.isPaused ? 'RESUME' : 'PAUSE',
                            style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 14),
                          ),
                        ),
                      ),
                      const SizedBox(width: 12),

                      // Stop / Finish
                      Expanded(
                        child: ElevatedButton.icon(
                          onPressed: _isSaving ? null : _handleStopRide,
                          style: ElevatedButton.styleFrom(
                            backgroundColor: Colors.redAccent,
                            foregroundColor: Colors.white,
                            padding: const EdgeInsets.symmetric(vertical: 16),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(16),
                            ),
                            elevation: 4,
                          ),
                          icon: _isSaving
                              ? const SizedBox(
                                  width: 18,
                                  height: 18,
                                  child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                                )
                              : const Icon(Icons.stop, size: 22),
                          label: const Text(
                            'FINISH',
                            style: TextStyle(fontWeight: FontWeight.w800, fontSize: 14),
                          ),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
