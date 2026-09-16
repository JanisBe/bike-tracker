import 'package:bike_tracker/models/ride.dart';
import 'package:bike_tracker/widgets/ride_card.dart';
import 'package:bike_tracker/widgets/stats_bar.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('StatsBar renders distance, time, and speed', (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(
          body: StatsBar(
            distanceKm: 18.5,
            duration: Duration(hours: 1, minutes: 12),
            speedKmh: 24.8,
            elevationMeters: 140,
          ),
        ),
      ),
    );

    expect(find.text('18.50'), findsOneWidget);
    expect(find.text('24.8'), findsOneWidget);
    expect(find.text('1:12:00'), findsOneWidget);
    expect(find.text('140 m'), findsOneWidget);
  });

  testWidgets('RideCard renders ride information correctly', (WidgetTester tester) async {
    final ride = Ride(
      id: 'test-1',
      userId: 'user-1',
      title: 'Sunday Morning Trail',
      startTime: DateTime(2026, 9, 16, 9, 0),
      endTime: DateTime(2026, 9, 16, 10, 30),
      distanceKm: 25.4,
      durationSeconds: 5400,
      avgSpeedKmh: 22.1,
      maxSpeedKmh: 35.0,
      elevationGain: 120,
      encodedPolyline: '',
    );

    bool tapped = false;

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: RideCard(
            ride: ride,
            onTap: () {
              tapped = true;
            },
          ),
        ),
      ),
    );

    expect(find.text('Sunday Morning Trail'), findsOneWidget);
    expect(find.text('25.40 km'), findsOneWidget);
    expect(find.text('1:30:00'), findsOneWidget);
    expect(find.text('22.1 km/h'), findsOneWidget);

    await tester.tap(find.byType(RideCard));
    expect(tapped, isTrue);
  });
}
