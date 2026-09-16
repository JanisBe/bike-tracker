import 'package:flutter/material.dart';
import '../models/ride.dart';
import '../utils/constants.dart';
import '../utils/formatters.dart';

class RideCard extends StatelessWidget {
  final Ride ride;
  final VoidCallback onTap;
  final VoidCallback? onDelete;

  const RideCard({
    super.key,
    required this.ride,
    required this.onTap,
    this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: AppConstants.cardBg,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppConstants.cardBorder),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.2),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(16),
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Top row: Title and Date
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            ride.title,
                            style: const TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.bold,
                              color: AppConstants.textPrimary,
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                          const SizedBox(height: 2),
                          Text(
                            Formatters.formatFriendlyDate(ride.startTime),
                            style: const TextStyle(
                              fontSize: 12,
                              color: AppConstants.textMuted,
                            ),
                          ),
                        ],
                      ),
                    ),
                    if (onDelete != null)
                      IconButton(
                        icon: const Icon(Icons.delete_outline, size: 20),
                        color: Colors.redAccent.withValues(alpha: 0.7),
                        onPressed: onDelete,
                        tooltip: 'Delete ride',
                        constraints: const BoxConstraints(),
                        padding: EdgeInsets.zero,
                      ),
                  ],
                ),
                const SizedBox(height: 14),
                const Divider(color: AppConstants.cardBorder, height: 1),
                const SizedBox(height: 14),

                // Metrics row
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    _buildMetric(
                      'DISTANCE',
                      Formatters.formatDistance(ride.distanceKm),
                      AppConstants.primaryColor,
                    ),
                    _buildMetric(
                      'TIME',
                      Formatters.formatDuration(ride.duration),
                      AppConstants.textPrimary,
                    ),
                    _buildMetric(
                      'AVG SPEED',
                      Formatters.formatSpeed(ride.avgSpeedKmh),
                      AppConstants.primaryDark,
                    ),
                    if (ride.elevationGain != null)
                      _buildMetric(
                        'ELEVATION',
                        Formatters.formatElevation(ride.elevationGain),
                        AppConstants.accentOrange,
                      ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildMetric(String label, String value, Color valueColor) {
    return Column(
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
            fontSize: 15,
            fontWeight: FontWeight.w700,
            color: valueColor,
          ),
        ),
      ],
    );
  }
}
