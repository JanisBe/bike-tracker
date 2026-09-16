import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/ride.dart';
import '../services/auth_service.dart';
import '../services/location_service.dart';
import '../services/ride_service.dart';
import '../utils/constants.dart';
import '../utils/formatters.dart';
import '../widgets/ride_card.dart';
import 'ride_detail_screen.dart';
import 'tracking_screen.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  void _startNewRide(BuildContext context) async {
    final locService = context.read<LocationService>();
    final hasPerm = await locService.checkAndRequestPermissions();

    if (!hasPerm) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Location permission is required to track your rides.'),
            backgroundColor: Colors.redAccent,
          ),
        );
      }
      return;
    }

    if (context.mounted) {
      Navigator.push(
        context,
        MaterialPageRoute(builder: (_) => const TrackingScreen()),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final authService = context.watch<AuthService>();
    final rideService = context.read<RideService>();
    final user = authService.currentUser;
    final userId = user?.uid ?? 'anonymous_user';
    final displayName = user?.displayName ?? (user?.isAnonymous == true ? 'Guest Rider' : 'Cyclist');

    return Scaffold(
      backgroundColor: AppConstants.darkBg,
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Header: User info & Logout
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 16, 20, 12),
              child: Row(
                children: [
                  CircleAvatar(
                    radius: 22,
                    backgroundColor: AppConstants.cardBg,
                    backgroundImage: user?.photoURL != null ? NetworkImage(user!.photoURL!) : null,
                    child: user?.photoURL == null
                        ? const Icon(Icons.person, color: AppConstants.primaryColor)
                        : null,
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          displayName,
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                            color: AppConstants.textPrimary,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        Text(
                          user?.email ?? 'Guest',
                          style: const TextStyle(
                            fontSize: 12,
                            color: AppConstants.textMuted,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    icon: const Icon(Icons.logout, color: AppConstants.textMuted),
                    tooltip: 'Sign Out',
                    onPressed: () async {
                      Navigator.of(context).popUntil((route) => route.isFirst);
                      await authService.signOut();
                    },
                  ),
                ],
              ),
            ),

            // Rides stream
            Expanded(
              child: StreamBuilder<List<Ride>>(
                stream: rideService.streamUserRides(userId),
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Center(
                      child: CircularProgressIndicator(color: AppConstants.primaryColor),
                    );
                  }

                  if (snapshot.hasError) {
                    return Center(
                      child: Padding(
                        padding: const EdgeInsets.all(24.0),
                        child: Text(
                          'Error loading rides: ${snapshot.error}',
                          textAlign: TextAlign.center,
                          style: const TextStyle(color: Colors.redAccent),
                        ),
                      ),
                    );
                  }

                  final rides = snapshot.data ?? [];

                  // Calculate summary stats
                  final totalKm = rides.fold<double>(0.0, (sum, r) => sum + r.distanceKm);
                  final totalSeconds = rides.fold<int>(0, (sum, r) => sum + r.durationSeconds);

                  return CustomScrollView(
                    slivers: [
                      // Summary KPI Banner
                      SliverToBoxAdapter(
                        child: Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 20.0, vertical: 8.0),
                          child: Container(
                            padding: const EdgeInsets.all(18),
                            decoration: BoxDecoration(
                              gradient: LinearGradient(
                                colors: [
                                  AppConstants.cardBg,
                                  AppConstants.cardBg.withValues(alpha: 0.8),
                                ],
                                begin: Alignment.topLeft,
                                end: Alignment.bottomRight,
                              ),
                              borderRadius: BorderRadius.circular(20),
                              border: Border.all(color: AppConstants.cardBorder),
                            ),
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.spaceAround,
                              children: [
                                _kpiItem('${rides.length}', 'TOTAL RIDES', AppConstants.primaryColor),
                                Container(width: 1, height: 36, color: AppConstants.cardBorder),
                                _kpiItem('${totalKm.toStringAsFixed(1)} km', 'TOTAL DISTANCE', AppConstants.primaryDark),
                                Container(width: 1, height: 36, color: AppConstants.cardBorder),
                                _kpiItem(
                                  Formatters.formatDuration(Duration(seconds: totalSeconds)),
                                  'TOTAL TIME',
                                  AppConstants.textPrimary,
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),

                      // Section Title
                      SliverToBoxAdapter(
                        child: Padding(
                          padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
                          child: Text(
                            'Recorded Rides (${rides.length})',
                            style: const TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.w700,
                              color: AppConstants.textMuted,
                              letterSpacing: 0.5,
                            ),
                          ),
                        ),
                      ),

                      // Empty State
                      if (rides.isEmpty)
                        SliverFillRemaining(
                          hasScrollBody: false,
                          child: Center(
                            child: Padding(
                              padding: const EdgeInsets.all(32.0),
                              child: Column(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Container(
                                    padding: const EdgeInsets.all(20),
                                    decoration: BoxDecoration(
                                      color: AppConstants.cardBg,
                                      shape: BoxShape.circle,
                                      border: Border.all(color: AppConstants.cardBorder),
                                    ),
                                    child: const Icon(
                                      Icons.directions_bike,
                                      size: 48,
                                      color: AppConstants.textMuted,
                                    ),
                                  ),
                                  const SizedBox(height: 16),
                                  const Text(
                                    'No rides tracked yet',
                                    style: TextStyle(
                                      fontSize: 18,
                                      fontWeight: FontWeight.bold,
                                      color: AppConstants.textPrimary,
                                    ),
                                  ),
                                  const SizedBox(height: 8),
                                  const Text(
                                    'Tap "Start Ride" below when you\'re ready to hit the road and record your GPX trail!',
                                    textAlign: TextAlign.center,
                                    style: TextStyle(
                                      fontSize: 13,
                                      color: AppConstants.textMuted,
                                      height: 1.4,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        )
                      else
                        // Rides List
                        SliverPadding(
                          padding: const EdgeInsets.symmetric(horizontal: 20.0, vertical: 8.0),
                          sliver: SliverList(
                            delegate: SliverChildBuilderDelegate(
                              (context, index) {
                                final ride = rides[index];
                                return RideCard(
                                  ride: ride,
                                  onTap: () {
                                    Navigator.push(
                                      context,
                                      MaterialPageRoute(
                                        builder: (_) => RideDetailScreen(ride: ride),
                                      ),
                                    );
                                  },
                                  onDelete: () async {
                                    await rideService.deleteRide(ride.id);
                                  },
                                );
                              },
                              childCount: rides.length,
                            ),
                          ),
                        ),
                    ],
                  );
                },
              ),
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton.extended(
        backgroundColor: AppConstants.primaryColor,
        foregroundColor: Colors.black,
        elevation: 6,
        onPressed: () => _startNewRide(context),
        icon: const Icon(Icons.play_arrow, size: 28),
        label: const Text(
          'Start Ride',
          style: TextStyle(fontWeight: FontWeight.w900, fontSize: 15),
        ),
      ),
    );
  }

  Widget _kpiItem(String value, String label, Color accent) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          value,
          style: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.w900,
            color: accent,
          ),
        ),
        const SizedBox(height: 2),
        Text(
          label,
          style: const TextStyle(
            fontSize: 9,
            fontWeight: FontWeight.bold,
            color: AppConstants.textMuted,
            letterSpacing: 0.5,
          ),
        ),
      ],
    );
  }
}
