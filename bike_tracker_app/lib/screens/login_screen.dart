import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/auth_service.dart';
import '../utils/constants.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  bool _isSignUp = false;
  bool _obscurePassword = true;

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  void _submitEmailAuth() async {
    final authService = context.read<AuthService>();
    // Sanitize input: trim whitespace, fix accidental typos like '.,' or ','
    var email = _emailController.text.trim();
    email = email.replaceAll(RegExp(r'\.,+'), '.').replaceAll(',', '.');
    final password = _passwordController.text;

    if (_isSignUp) {
      await authService.signUpWithEmail(email, password);
    } else {
      await authService.signInWithEmail(email, password);
    }
  }

  @override
  Widget build(BuildContext context) {
    final authService = context.watch<AuthService>();

    return Scaffold(
      backgroundColor: AppConstants.darkBg,
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 28.0, vertical: 20.0),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // Logo & App Icon
                Center(
                  child: Container(
                    width: 84,
                    height: 84,
                    decoration: BoxDecoration(
                      color: AppConstants.cardBg,
                      shape: BoxShape.circle,
                      border: Border.all(color: AppConstants.primaryColor, width: 2),
                      boxShadow: [
                        BoxShadow(
                          color: AppConstants.primaryColor.withValues(alpha: 0.25),
                          blurRadius: 20,
                          spreadRadius: 2,
                        ),
                      ],
                    ),
                    child: const Center(
                      child: Icon(
                        Icons.directions_bike,
                        size: 44,
                        color: AppConstants.primaryColor,
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 18),

                // Title and Tagline
                const Text(
                  AppConstants.appName,
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 28,
                    fontWeight: FontWeight.w900,
                    color: AppConstants.textPrimary,
                    letterSpacing: -0.5,
                  ),
                ),
                const SizedBox(height: 6),
                const Text(
                  'Live GPS Tracking • Clean GPX Telemetry • Spark Cloud',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 12,
                    color: AppConstants.textMuted,
                  ),
                ),
                const SizedBox(height: 28),

                // Error notification if present
                if (authService.errorMessage != null) ...[
                  Container(
                    padding: const EdgeInsets.all(12),
                    margin: const EdgeInsets.only(bottom: 16),
                    decoration: BoxDecoration(
                      color: Colors.red.withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: Colors.redAccent.withValues(alpha: 0.5)),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.error_outline, color: Colors.redAccent, size: 20),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            authService.errorMessage!,
                            style: const TextStyle(color: Colors.redAccent, fontSize: 13),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],

                // Email Input
                TextField(
                  controller: _emailController,
                  keyboardType: TextInputType.emailAddress,
                  style: const TextStyle(color: AppConstants.textPrimary),
                  decoration: InputDecoration(
                    labelText: 'Email',
                    hintText: 'name@example.com',
                    labelStyle: const TextStyle(color: AppConstants.textMuted),
                    hintStyle: TextStyle(color: AppConstants.textMuted.withValues(alpha: 0.5)),
                    prefixIcon: const Icon(Icons.email_outlined, color: AppConstants.textMuted),
                    filled: true,
                    fillColor: AppConstants.cardBg,
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(14),
                      borderSide: const BorderSide(color: AppConstants.cardBorder),
                    ),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(14),
                      borderSide: const BorderSide(color: AppConstants.cardBorder),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(14),
                      borderSide: const BorderSide(color: AppConstants.primaryColor),
                    ),
                  ),
                ),
                const SizedBox(height: 12),

                // Password Input
                TextField(
                  controller: _passwordController,
                  obscureText: _obscurePassword,
                  style: const TextStyle(color: AppConstants.textPrimary),
                  decoration: InputDecoration(
                    labelText: 'Password',
                    labelStyle: const TextStyle(color: AppConstants.textMuted),
                    prefixIcon: const Icon(Icons.lock_outline, color: AppConstants.textMuted),
                    suffixIcon: IconButton(
                      icon: Icon(
                        _obscurePassword ? Icons.visibility_off : Icons.visibility,
                        color: AppConstants.textMuted,
                      ),
                      onPressed: () {
                        setState(() => _obscurePassword = !_obscurePassword);
                      },
                    ),
                    filled: true,
                    fillColor: AppConstants.cardBg,
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(14),
                      borderSide: const BorderSide(color: AppConstants.cardBorder),
                    ),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(14),
                      borderSide: const BorderSide(color: AppConstants.cardBorder),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(14),
                      borderSide: const BorderSide(color: AppConstants.primaryColor),
                    ),
                  ),
                ),
                const SizedBox(height: 16),

                // Primary Submit Button (Sign In / Sign Up)
                ElevatedButton(
                  onPressed: authService.isLoading ? null : _submitEmailAuth,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppConstants.primaryColor,
                    foregroundColor: Colors.black,
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(14),
                    ),
                  ),
                  child: authService.isLoading
                      ? const SizedBox(
                          width: 20,
                          height: 20,
                          child: CircularProgressIndicator(strokeWidth: 2, color: Colors.black),
                        )
                      : Text(
                          _isSignUp ? 'Create Account' : 'Sign In',
                          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                        ),
                ),

                // Toggle Sign In / Sign Up mode
                TextButton(
                  onPressed: () {
                    setState(() {
                      _isSignUp = !_isSignUp;
                    });
                    authService.clearError();
                  },
                  child: Text(
                    _isSignUp
                        ? 'Already have an account? Sign in'
                        : "Don't have an account? Create one",
                    style: const TextStyle(color: AppConstants.primaryDark, fontSize: 13),
                  ),
                ),

                const SizedBox(height: 12),

                // Divider
                Row(
                  children: [
                    const Expanded(child: Divider(color: AppConstants.cardBorder)),
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 12.0),
                      child: Text(
                        'or',
                        style: TextStyle(color: AppConstants.textMuted.withValues(alpha: 0.7), fontSize: 12),
                      ),
                    ),
                    const Expanded(child: Divider(color: AppConstants.cardBorder)),
                  ],
                ),

                const SizedBox(height: 16),

                // Google Sign-In Button
                ElevatedButton.icon(
                  onPressed: authService.isLoading
                      ? null
                      : () async {
                          await authService.signInWithGoogle();
                        },
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.white,
                    foregroundColor: Colors.black87,
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(14),
                    ),
                    elevation: 1,
                  ),
                  icon: Image.network(
                    'https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg',
                    width: 20,
                    height: 20,
                    errorBuilder: (context, error, stackTrace) =>
                        const Icon(Icons.g_mobiledata, size: 26),
                  ),
                  label: const Text(
                    'Sign in with Google',
                    style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
                  ),
                ),

                const SizedBox(height: 10),

                // Guest / Anonymous button
                OutlinedButton.icon(
                  onPressed: authService.isLoading
                      ? null
                      : () async {
                          await authService.signInAnonymously();
                        },
                  style: OutlinedButton.styleFrom(
                    foregroundColor: AppConstants.textPrimary,
                    side: const BorderSide(color: AppConstants.cardBorder),
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(14),
                    ),
                  ),
                  icon: const Icon(Icons.person_outline, size: 20, color: AppConstants.textMuted),
                  label: const Text(
                    'Continue as Guest',
                    style: TextStyle(fontSize: 14, fontWeight: FontWeight.w500),
                  ),
                ),

                const SizedBox(height: 20),
                const Center(
                  child: Text(
                    '100% Free & Open-Source • Spark Tier',
                    style: TextStyle(fontSize: 11, color: AppConstants.textMuted),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
