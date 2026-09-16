import 'package:flutter/material.dart';
import '../utils/constants.dart';
import '../utils/formatters.dart';

class StatsBar extends StatelessWidget {
  final double distanceKm;
  final Duration duration;
  final double speedKmh;
  final double? elevationMeters;
  final bool isCompact;

  const StatsBar({
    super.key,
    required this.distanceKm,
    required this.duration,
    required this.speedKmh,
    this.elevationMeters,
    this.isCompact = false,
  });

  @override
  Widget build(BuildContext context) {
    if (isCompact) {
      return Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          color: AppConstants.cardBg.withValues(alpha: 0.92),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: AppConstants.cardBorder),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceAround,
          children: [
            _buildStatItem('DISTANCE', Formatters.formatDistance(distanceKm), Icons.straighten),
            _buildDivider(),
            _buildStatItem('TIME', Formatters.formatDuration(duration), Icons.timer_outlined),
            _buildDivider(),
            _buildStatItem('SPEED', Formatters.formatSpeed(speedKmh), Icons.speed),
          ],
        ),
      );
    }

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppConstants.cardBg.withValues(alpha: 0.95),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: AppConstants.cardBorder),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.4),
            blurRadius: 16,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              _buildLargeStatItem(
                'SPEED',
                speedKmh.toStringAsFixed(1),
                'km/h',
                AppConstants.primaryColor,
              ),
              _buildDivider(height: 48),
              _buildLargeStatItem(
                'DISTANCE',
                distanceKm.toStringAsFixed(2),
                'km',
                AppConstants.primaryDark,
              ),
            ],
          ),
          const SizedBox(height: 12),
          const Divider(color: AppConstants.cardBorder, height: 1),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              _buildSubStatItem(
                'DURATION',
                Formatters.formatDuration(duration),
                Icons.timer_outlined,
              ),
              if (elevationMeters != null)
                _buildSubStatItem(
                  'ELEVATION',
                  Formatters.formatElevation(elevationMeters),
                  Icons.landscape_outlined,
                ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildStatItem(String label, String value, IconData icon) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 14, color: AppConstants.textMuted),
            const SizedBox(width: 4),
            Text(
              label,
              style: const TextStyle(
                fontSize: 10,
                fontWeight: FontWeight.w600,
                color: AppConstants.textMuted,
                letterSpacing: 0.5,
              ),
            ),
          ],
        ),
        const SizedBox(height: 4),
        Text(
          value,
          style: const TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.bold,
            color: AppConstants.textPrimary,
          ),
        ),
      ],
    );
  }

  Widget _buildLargeStatItem(String label, String value, String unit, Color accent) {
    return Expanded(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            label,
            style: const TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.w600,
              color: AppConstants.textMuted,
              letterSpacing: 1.0,
            ),
          ),
          const SizedBox(height: 4),
          Row(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.baseline,
            textBaseline: TextBaseline.alphabetic,
            children: [
              Text(
                value,
                style: TextStyle(
                  fontSize: 32,
                  fontWeight: FontWeight.w900,
                  color: accent,
                  letterSpacing: -0.5,
                ),
              ),
              const SizedBox(width: 4),
              Text(
                unit,
                style: const TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                  color: AppConstants.textMuted,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildSubStatItem(String label, String value, IconData icon) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 16, color: AppConstants.textMuted),
        const SizedBox(width: 6),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              label,
              style: const TextStyle(
                fontSize: 9,
                fontWeight: FontWeight.bold,
                color: AppConstants.textMuted,
                letterSpacing: 0.5,
              ),
            ),
            Text(
              value,
              style: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.bold,
                color: AppConstants.textPrimary,
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildDivider({double height = 24}) {
    return Container(
      width: 1,
      height: height,
      color: AppConstants.cardBorder,
    );
  }
}
