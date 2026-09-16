import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/foundation.dart';
import 'package:google_sign_in/google_sign_in.dart';

class AuthService extends ChangeNotifier {
  final FirebaseAuth _auth = FirebaseAuth.instance;
  final GoogleSignIn _googleSignIn = GoogleSignIn.instance;
  bool _initializedGoogle = false;

  User? get currentUser => _auth.currentUser;
  bool get isAuthenticated => currentUser != null;
  Stream<User?> get authStateChanges => _auth.authStateChanges();

  bool _isLoading = false;
  bool get isLoading => _isLoading;

  String? _errorMessage;
  String? get errorMessage => _errorMessage;

  void clearError() {
    _errorMessage = null;
    notifyListeners();
  }

  Future<void> _ensureGoogleInitialized() async {
    if (!_initializedGoogle) {
      try {
        await _googleSignIn.initialize();
      } catch (e) {
        debugPrint('GoogleSignIn initialize warning: $e');
      }
      _initializedGoogle = true;
    }
  }

  /// Sign in with Google Account using google_sign_in 7.x API
  Future<UserCredential?> signInWithGoogle() async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      await _ensureGoogleInitialized();
      final GoogleSignInAccount googleAccount = await _googleSignIn.authenticate();
      final GoogleSignInAuthentication googleAuth = googleAccount.authentication;

      final AuthCredential credential = GoogleAuthProvider.credential(
        idToken: googleAuth.idToken,
      );

      final UserCredential userCredential = await _auth.signInWithCredential(credential);
      _isLoading = false;
      notifyListeners();
      return userCredential;
    } catch (e) {
      _errorMessage = 'Google Sign-In failed: $e';
      _isLoading = false;
      notifyListeners();
      return null;
    }
  }

  /// Sign in anonymously for testing and guest access
  Future<UserCredential?> signInAnonymously() async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final UserCredential userCredential = await _auth.signInAnonymously();
      _isLoading = false;
      notifyListeners();
      return userCredential;
    } catch (e) {
      _errorMessage = 'Anonymous sign-in failed: $e';
      _isLoading = false;
      notifyListeners();
      return null;
    }
  }

  /// Sign in with Email and Password
  Future<UserCredential?> signInWithEmail(String email, String password) async {
    final cleanEmail = email.trim().replaceAll(',', '.');
    if (cleanEmail.isEmpty || password.isEmpty) {
      _errorMessage = 'Email and password cannot be empty';
      notifyListeners();
      return null;
    }

    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final credential = await _auth.signInWithEmailAndPassword(
        email: cleanEmail,
        password: password,
      );
      _isLoading = false;
      notifyListeners();
      return credential;
    } on FirebaseAuthException catch (e) {
      _errorMessage = e.message ?? 'Sign-in failed';
      _isLoading = false;
      notifyListeners();
      return null;
    } catch (e) {
      _errorMessage = 'Sign-in failed: $e';
      _isLoading = false;
      notifyListeners();
      return null;
    }
  }

  /// Register / Sign up with Email and Password
  Future<UserCredential?> signUpWithEmail(String email, String password) async {
    final cleanEmail = email.trim().replaceAll(',', '.');
    if (cleanEmail.isEmpty || !cleanEmail.contains('@') || !cleanEmail.contains('.')) {
      _errorMessage = 'Please enter a valid email address';
      notifyListeners();
      return null;
    }

    if (password.length < 6) {
      _errorMessage = 'Password must be at least 6 characters';
      notifyListeners();
      return null;
    }

    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final credential = await _auth.createUserWithEmailAndPassword(
        email: cleanEmail,
        password: password,
      );
      _isLoading = false;
      notifyListeners();
      return credential;
    } on FirebaseAuthException catch (e) {
      _errorMessage = e.message ?? 'Sign-up failed';
      _isLoading = false;
      notifyListeners();
      return null;
    } catch (e) {
      _errorMessage = 'Sign-up failed: $e';
      _isLoading = false;
      notifyListeners();
      return null;
    }
  }

  /// Sign out
  Future<void> signOut() async {
    _isLoading = true;
    notifyListeners();

    try {
      // 1. Always sign out from Firebase Auth first so auth state listener updates immediately
      await _auth.signOut();
    } catch (e) {
      debugPrint('FirebaseAuth signOut error: $e');
    }

    try {
      // 2. Safely attempt to sign out from Google
      await _googleSignIn.signOut();
    } catch (e) {
      debugPrint('GoogleSignIn signOut warning: $e');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
}
