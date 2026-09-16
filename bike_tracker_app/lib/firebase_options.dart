// File generated based on Firebase configuration for bike-tracker-97e13.
import 'package:firebase_core/firebase_core.dart' show FirebaseOptions;
import 'package:flutter/foundation.dart'
    show defaultTargetPlatform, kIsWeb, TargetPlatform;

/// Default [FirebaseOptions] for use with your Firebase apps.
class DefaultFirebaseOptions {
  static FirebaseOptions get currentPlatform {
    if (kIsWeb) {
      return web;
    }
    switch (defaultTargetPlatform) {
      case TargetPlatform.android:
        return android;
      case TargetPlatform.iOS:
        return ios;
      case TargetPlatform.macOS:
        return macos;
      case TargetPlatform.windows:
        return windows;
      case TargetPlatform.linux:
        throw UnsupportedError(
          'DefaultFirebaseOptions have not been configured for linux.',
        );
      default:
        throw UnsupportedError(
          'DefaultFirebaseOptions are not supported for this platform.',
        );
    }
  }

  static const FirebaseOptions web = FirebaseOptions(
    apiKey: 'AIzaSyCQkHkh7UxgA5mzY8p3ZhGrJ3pZ6Lpz7tk',
    appId: '1:654235244168:web:7d131cb98a0d128b1cf13d',
    messagingSenderId: '654235244168',
    projectId: 'bike-tracker-97e13',
    authDomain: 'bike-tracker-97e13.firebaseapp.com',
    storageBucket: 'bike-tracker-97e13.firebasestorage.app',
  );

  static const FirebaseOptions android = FirebaseOptions(
    apiKey: 'AIzaSyDs-6wzyTCe08CYYuw5LkEumMKcOlWnpSk',
    appId: '1:654235244168:android:02054ebdfe9c88da1cf13d',
    messagingSenderId: '654235244168',
    projectId: 'bike-tracker-97e13',
    storageBucket: 'bike-tracker-97e13.firebasestorage.app',
  );

  static const FirebaseOptions ios = FirebaseOptions(
    apiKey: 'AIzaSyDs-6wzyTCe08CYYuw5LkEumMKcOlWnpSk',
    appId: '1:654235244168:ios:02054ebdfe9c88da1cf13d',
    messagingSenderId: '654235244168',
    projectId: 'bike-tracker-97e13',
    storageBucket: 'bike-tracker-97e13.firebasestorage.app',
  );

  static const FirebaseOptions macos = FirebaseOptions(
    apiKey: 'AIzaSyDs-6wzyTCe08CYYuw5LkEumMKcOlWnpSk',
    appId: '1:654235244168:ios:02054ebdfe9c88da1cf13d',
    messagingSenderId: '654235244168',
    projectId: 'bike-tracker-97e13',
    storageBucket: 'bike-tracker-97e13.firebasestorage.app',
  );

  static const FirebaseOptions windows = FirebaseOptions(
    apiKey: 'AIzaSyCQkHkh7UxgA5mzY8p3ZhGrJ3pZ6Lpz7tk',
    appId: '1:654235244168:web:7d131cb98a0d128b1cf13d',
    messagingSenderId: '654235244168',
    projectId: 'bike-tracker-97e13',
    authDomain: 'bike-tracker-97e13.firebaseapp.com',
    storageBucket: 'bike-tracker-97e13.firebasestorage.app',
  );
}
