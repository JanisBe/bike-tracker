import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'firebase_options.dart';
import 'screens/home_screen.dart';
import 'screens/login_screen.dart';
import 'services/auth_service.dart';
import 'services/location_service.dart';
import 'services/ride_service.dart';
import 'utils/constants.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  try {
    await Firebase.initializeApp(
      options: DefaultFirebaseOptions.currentPlatform,
    );
  } catch (e) {
    debugPrint('Firebase initialization error: $e');
  }

  runApp(const BikeTrackerApp());
}

class BikeTrackerApp extends StatelessWidget {
  const BikeTrackerApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => AuthService()),
        ChangeNotifierProvider(create: (_) => LocationService()),
        Provider(create: (_) => RideService()),
      ],
      child: MaterialApp(
        title: AppConstants.appName,
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          useMaterial3: true,
          brightness: Brightness.dark,
          scaffoldBackgroundColor: AppConstants.darkBg,
          colorScheme: const ColorScheme.dark(
            primary: AppConstants.primaryColor,
            secondary: AppConstants.primaryDark,
            surface: AppConstants.cardBg,
          ),
          cardTheme: const CardThemeData(
            color: AppConstants.cardBg,
            elevation: 0,
          ),
          appBarTheme: const AppBarTheme(
            backgroundColor: AppConstants.darkBg,
            foregroundColor: AppConstants.textPrimary,
            elevation: 0,
          ),
        ),
        home: const AuthGate(),
      ),
    );
  }
}

class AuthGate extends StatelessWidget {
  const AuthGate({super.key});

  @override
  Widget build(BuildContext context) {
    final authService = context.watch<AuthService>();

    return StreamBuilder<User?>(
      stream: authService.authStateChanges,
      initialData: authService.currentUser,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting && !snapshot.hasData) {
          return const Scaffold(
            backgroundColor: AppConstants.darkBg,
            body: Center(
              child: CircularProgressIndicator(color: AppConstants.primaryColor),
            ),
          );
        }

        final user = snapshot.data;
        if (user != null) {
          return const HomeScreen();
        }

        return const LoginScreen();
      },
    );
  }
}
