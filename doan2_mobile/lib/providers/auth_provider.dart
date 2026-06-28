import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:flutter_facebook_auth/flutter_facebook_auth.dart';
import '../models/user.dart';
import '../services/api_service.dart';

class AuthProvider with ChangeNotifier {
  User? _user;
  final ApiService _apiService = ApiService();
  bool _isLoading = true;
  bool _googleSignInInitialized = false;

  User? get user => _user;
  bool get isAuthenticated => _user != null;
  bool get isLoading => _isLoading;

  AuthProvider() {
    _loadUserFromPrefs();
  }

  Future<void> _loadUserFromPrefs() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('token');
    final username = prefs.getString('username');
    final roles = prefs.getStringList('roles');
    final userCode = prefs.getString('userCode');

    if (token != null && username != null && roles != null) {
      _user = User(username: username, token: token, roles: roles, userCode: userCode);
    }
    _isLoading = false;
    notifyListeners();
  }

  Future<bool> login(String username, String password) async {
    try {
      final user = await _apiService.login(username, password);
      if (user != null) {
        _user = user;
        notifyListeners();
        return true;
      }
      return false;
    } catch (e) {
      print('Login error: $e');
      return false;
    }
  }

  Future<void> _ensureGoogleSignInInitialized() async {
    if (!_googleSignInInitialized) {
      await GoogleSignIn.instance.initialize(
        // Web Client ID (Không dùng Android Client ID ở đây)
        serverClientId: "533216430410-3f8da959gfvfjd3hhsvboot644gb1smg.apps.googleusercontent.com",
      );
      _googleSignInInitialized = true;
    }
  }

  Future<bool> loginWithGoogle() async {
    try {
      await _ensureGoogleSignInInitialized();
      final GoogleSignInAccount googleUser = await GoogleSignIn.instance.authenticate();

      final GoogleSignInAuthentication googleAuth = googleUser.authentication;
      final String? idToken = googleAuth.idToken;

      if (idToken != null) {
        final user = await _apiService.loginWithGoogle(idToken);
        if (user != null) {
          _user = user;
          notifyListeners();
          return true;
        }
      }
      return false;
    } catch (e) {
      print('Google Login error: $e');
      return false;
    }
  }

  Future<bool> loginWithFacebook() async {
    try {
      final LoginResult result = await FacebookAuth.instance.login();
      
      if (result.status == LoginStatus.success) {
        final AccessToken accessToken = result.accessToken!;
        final user = await _apiService.loginWithFacebook(accessToken.tokenString);
        
        if (user != null) {
          _user = user;
          notifyListeners();
          return true;
        }
      }
      return false;
    } catch (e) {
      print('Facebook Login error: $e');
      return false;
    }
  }

  Future<void> logout() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.clear();
    
    // Sign out from Google & Facebook if necessary
    try {
      if (_googleSignInInitialized) {
        await GoogleSignIn.instance.signOut();
      }
      await FacebookAuth.instance.logOut();
    } catch (e) {
      // Ignore errors if not logged in via these providers
    }

    _user = null;
    notifyListeners();
  }

  bool hasRole(String role) {
    if (_user == null) return false;
    return _user!.roles.contains(role);
  }
}
