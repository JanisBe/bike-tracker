import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';
import '../utils/constants.dart';

class LiveMap extends StatefulWidget {
  final List<LatLng> routePoints;
  final LatLng? currentLocation;
  final bool isTracking;
  final bool autoCenter;
  final LatLng? initialCenter;
  final double initialZoom;

  const LiveMap({
    super.key,
    required this.routePoints,
    this.currentLocation,
    this.isTracking = false,
    this.autoCenter = true,
    this.initialCenter,
    this.initialZoom = 15.5,
  });

  @override
  State<LiveMap> createState() => _LiveMapState();
}

class _LiveMapState extends State<LiveMap> {
  final MapController _mapController = MapController();
  bool _userPanned = false;

  @override
  void didUpdateWidget(covariant LiveMap oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (!_userPanned && widget.autoCenter && widget.currentLocation != null) {
      if (oldWidget.currentLocation != widget.currentLocation) {
        _mapController.move(widget.currentLocation!, _mapController.camera.zoom);
      }
    }
  }

  void _recenter() {
    final target = widget.currentLocation ??
        (widget.routePoints.isNotEmpty ? widget.routePoints.last : null);
    if (target != null) {
      setState(() {
        _userPanned = false;
      });
      _mapController.move(target, 16.0);
    }
  }

  @override
  Widget build(BuildContext context) {
    final defaultCenter = widget.currentLocation ??
        (widget.routePoints.isNotEmpty
            ? widget.routePoints.first
            : widget.initialCenter ?? const LatLng(52.2297, 21.0122)); // Warsaw fallback

    return Stack(
      children: [
        FlutterMap(
          mapController: _mapController,
          options: MapOptions(
            initialCenter: defaultCenter,
            initialZoom: widget.initialZoom,
            onPositionChanged: (camera, hasGesture) {
              if (hasGesture && !_userPanned) {
                setState(() {
                  _userPanned = true;
                });
              }
            },
          ),
          children: [
            TileLayer(
              urlTemplate: AppConstants.mapTileUrl,
              userAgentPackageName: AppConstants.mapUserAgentPackage,
              maxZoom: 19,
            ),
            // Route Polyline
            if (widget.routePoints.isNotEmpty)
              PolylineLayer(
                polylines: [
                  // Glow background line
                  Polyline(
                    points: widget.routePoints,
                    strokeWidth: 6.0,
                    color: AppConstants.primaryColor.withValues(alpha: 0.4),
                  ),
                  // Sharp foreground line
                  Polyline(
                    points: widget.routePoints,
                    strokeWidth: 3.5,
                    color: AppConstants.primaryColor,
                  ),
                ],
              ),
            // Markers: Start, End, Current
            MarkerLayer(
              markers: [
                // Start pin if route has points
                if (widget.routePoints.isNotEmpty)
                  Marker(
                    point: widget.routePoints.first,
                    width: 24,
                    height: 24,
                    child: Container(
                      decoration: BoxDecoration(
                        color: Colors.white,
                        shape: BoxShape.circle,
                        border: Border.all(color: Colors.green, width: 3),
                        boxShadow: const [
                          BoxShadow(color: Colors.black38, blurRadius: 4),
                        ],
                      ),
                      child: const Center(
                        child: Icon(Icons.play_arrow, size: 12, color: Colors.green),
                      ),
                    ),
                  ),
                // Current position marker
                if (widget.currentLocation != null)
                  Marker(
                    point: widget.currentLocation!,
                    width: 32,
                    height: 32,
                    child: Container(
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: AppConstants.primaryDark.withValues(alpha: 0.3),
                      ),
                      child: Center(
                        child: Container(
                          width: 16,
                          height: 16,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: AppConstants.primaryDark,
                            border: Border.all(color: Colors.white, width: 2.5),
                            boxShadow: const [
                              BoxShadow(color: Colors.black45, blurRadius: 6),
                            ],
                          ),
                        ),
                      ),
                    ),
                  ),
              ],
            ),
          ],
        ),

        // Recenter floating button
        if (_userPanned && (widget.currentLocation != null || widget.routePoints.isNotEmpty))
          Positioned(
            right: 16,
            bottom: 24,
            child: FloatingActionButton.small(
              backgroundColor: AppConstants.cardBg,
              foregroundColor: AppConstants.primaryColor,
              onPressed: _recenter,
              child: const Icon(Icons.my_location),
            ),
          ),
      ],
    );
  }
}
