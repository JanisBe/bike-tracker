import 'package:flutter/material.dart';
import 'package:latlong2/latlong.dart';
import 'package:provider/provider.dart';
import '../models/ride.dart';
import '../models/track_point.dart';
import '../services/gpx_service.dart';
import '../services/ride_service.dart';
import '../utils/constants.dart';
import '../utils/formatters.dart';
import '../utils/geo_utils.dart';
import '../widgets/live_map.dart';

class RideDetailScreen extends StatefulWidget {
  final Ride ride;

  const RideDetailScreen({super.key, required this.ride});

  @override
  State<RideDetailScreen> createState() => _RideDetailScreenState();
}

class _RideDetailScreenState extends State<RideDetailScreen> {
  final GpxService _gpxService = GpxService();
  bool _isExporting = false;
  late final List<LatLng> _routePoints;

  @override
  void initState() {
    super.initState();
    _routePoints = GeoUtils.decodePolyline(widget.ride.encodedPolyline);
  }

  Future<void> _exportGpx() async {
    setState(() => _isExporting = true);
    try {
      final rideService = context.read<RideService>();
      String? gpxContent = await rideService.fetchRideGpx(widget.ride.id);

      // If GPX not found in details/gpx subcollection, generate from polyline points
      if (gpxContent == null || gpxContent.isEmpty) {
        final dummyTrackPoints = _routePoints.map((ll) {
          return TrackPoint(
            latitude: ll.latitude,
            longitude: ll.longitude,
            timestamp: widget.ride.startTime,
          );
        }).toList();
        gpxContent = _gpxService.generateGpxString(
          dummyTrackPoints,
          trackName: widget.ride.title,
          startTime: widget.ride.startTime,
        );
      }

      final safeName = widget.ride.title.replaceAll(RegExp(r'[^\w\s-]'), '_');
      await _gpxService.shareGpx(gpxContent, '$safeName.gpx');
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to export GPX: $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _isExporting = false);
    }
  }

  Future<void> _confirmDelete() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppConstants.cardBg,
        title: const Text('Delete Ride', style: TextStyle(color: AppConstants.textPrimary)),
        content: const Text(
          'Are you sure you want to delete this ride permanently?',
          style: TextStyle(color: AppConstants.textMuted),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.redAccent),
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Delete'),
          ),
        ],
      ),
    );

    if (confirmed == true && mounted) {
      final rideService = context.read<RideService>();
      await rideService.deleteRide(widget.ride.id);
      if (mounted) {
        Navigator.pop(context);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Ride deleted')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppConstants.darkBg,
      appBar: AppBar(
        backgroundColor: AppConstants.darkBg,
        elevation: 0,
        title: Text(
          widget.ride.title,
          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.delete_outline, color: Colors.redAccent),
            onPressed: _confirmDelete,
            tooltip: 'Delete Ride',
          ),
        ],
      ),
      body: Column(
        children: [
          // Route Map
          Expanded(
            flex: 4,
            child: Stack(
              children: [
                LiveMap(
                  routePoints: _routePoints,
                  currentLocation: _routePoints.isNotEmpty ? _routePoints.last : null,
                  initialCenter: _routePoints.isNotEmpty ? _routePoints.first : null,
                  initialZoom: 14.0,
                  autoCenter: false,
                ),
                Positioned(
                  top: 12,
                  left: 12,
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                    decoration: BoxDecoration(
                      color: AppConstants.darkBg.withValues(alpha: 0.85),
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: AppConstants.cardBorder),
                    ),
                    child: Text(
                      '${_routePoints.length} GPS Points',
                      style: const TextStyle(fontSize: 11, color: AppConstants.textMuted),
                    ),
                  ),
                ),
              ],
            ),
          ),

          // Ride Summary Sheet
          Expanded(
            flex: 5,
            child: Container(
              padding: const EdgeInsets.all(20),
              decoration: const BoxDecoration(
                color: AppConstants.darkBg,
                borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
              ),
              child: SingleChildScrollView(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    // Date & Time
                    Row(
                      children: [
                        const Icon(Icons.calendar_today_outlined,
                            size: 16, color: AppConstants.primaryColor),
                        const SizedBox(width: 8),
                        Text(
                          Formatters.formatFriendlyDate(widget.ride.startTime),
                          style: const TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w600,
                            color: AppConstants.textPrimary,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),

                    // Grid of Metrics
                    Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: AppConstants.cardBg,
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(color: AppConstants.cardBorder),
                      ),
                      child: Column(
                        children: [
                          Row(
                            children: [
                              _buildMetricItem('DISTANCE', Formatters.formatDistance(widget.ride.distanceKm), AppConstants.primaryColor),
                              _buildMetricItem('DURATION', Formatters.formatDuration(widget.ride.duration), AppConstants.textPrimary),
                            ],
                          ),
                          const Divider(color: AppConstants.cardBorder, height: 24),
                          Row(
                            children: [
                              _buildMetricItem('AVG SPEED', Formatters.formatSpeed(widget.ride.avgSpeedKmh), AppConstants.primaryDark),
                              _buildMetricItem('MAX SPEED', Formatters.formatSpeed(widget.ride.maxSpeedKmh), AppConstants.textPrimary),
                            ],
                          ),
                          if (widget.ride.elevationGain != null) ...[
                            const Divider(color: AppConstants.cardBorder, height: 24),
                            Row(
                              children: [
                                _buildMetricItem('ELEVATION GAIN', Formatters.formatElevation(widget.ride.elevationGain), AppConstants.accentOrange),
                                _buildMetricItem('START TIME', Formatters.formatDateTime(widget.ride.startTime).split(' ').last, AppConstants.textMuted),
                              ],
                            ),
                          ],
                        ],
                      ),
                    ),
                    const SizedBox(height: 20),

                    // Export / Share GPX button
                    ElevatedButton.icon(
                      onPressed: _isExporting ? null : _exportGpx,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppConstants.primaryColor,
                        foregroundColor: Colors.black,
                        padding: const EdgeInsets.symmetric(vertical: 16),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(14),
                        ),
                      ),
                      icon: _isExporting
                          ? const SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                                color: Colors.black,
                              ),
                            )
                          : const Icon(Icons.share, size: 20),
                      label: const Text(
                        'Export / Share GPX File',
                        style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMetricItem(String label, String value, Color color) {
    return Expanded(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: const TextStyle(
              fontSize: 10,
              fontWeight: FontWeight.w600,
              color: AppConstants.textMuted,
              letterSpacing: 0.5,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            value,
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              color: color,
            ),
          ),
        ],
      ),
    );
  }
}
