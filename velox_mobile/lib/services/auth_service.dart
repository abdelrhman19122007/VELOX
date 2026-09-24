import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import '../core/api_client.dart';
import '../core/app_config.dart';
import '../models/profile.dart';

/// Auth flow mirroring the web `authService.js`:
/// register -> (pending, otp) -> verify-otp -> login -> profile -> logout.
class AuthService {
  AuthService._();

  static final AuthService instance = AuthService._();

  static const _sessionKey = 'velox_session';

  Profile? _profile;

  Profile? get profile => _profile;

  bool get isLoggedIn => _profile != null;

  Future<void> restore() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_sessionKey);
    if (raw == null) return;
    try {
      final data = jsonDecode(raw) as Map<String, dynamic>;
      ApiClient.instance.token = data['token'] as String?;
      _profile = Profile.fromJson(
          Map<String, dynamic>.from(data['user'] as Map<String, dynamic>? ?? {}));
    } catch (_) {
      await prefs.remove(_sessionKey);
      _profile = null;
      ApiClient.instance.token = null;
    }
  }

  Future<void> _saveSession(String token, Profile user) async {
    ApiClient.instance.token = token;
    _profile = user;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(
        _sessionKey,
        jsonEncode({
          'token': token,
          'user': {
            'id': user.id,
            'full_name': user.fullName,
            'email': user.email,
            'phone_number': user.phone,
            'governorate': user.governorate,
            'role': user.role,
          }
        }));
  }

  /// register -> returns {pending: true, otp} when the account must be OTP
  /// verified first (exactly like the web).
  Future<Map<String, dynamic>> register({
    required String email,
    required String password,
    required String fullName,
    required String phone,
    required String governorate,
  }) async {
    if (useMockApi) {
      return _mockRegister(email, password, fullName, phone, governorate);
    }
    final data = await ApiClient.instance.request(
      AppEndpoints.register,
      method: 'POST',
      body: {
        'email': email.trim(),
        'password': password,
        'full_name': fullName.trim(),
        'phone_number': phone.replaceAll(' ', ''),
        'governorate': governorate,
      },
    );
    if (data is Map && (data['pending'] == true || data['otp'] != null)) {
      return {
        'pending': true,
        'email': email.trim(),
        'otp': data['otp'],
      };
    }
    if (data is Map && data['token'] != null && data['user'] != null) {
      final user = Profile.fromJson(
          Map<String, dynamic>.from(data['user'] as Map<String, dynamic>));
      await _saveSession(data['token'].toString(), user);
      return {'pending': false, 'user': user};
    }
    throw ApiException('استجابة تسجيل غير متوقعة');
  }

  Future<Profile> verifyOtp({required String email, required String code}) async {
    if (useMockApi) {
      await Future.delayed(const Duration(milliseconds: mockLatencyMs));
      return _mockProfile(email);
    }
    final data = await ApiClient.instance.request(
      AppEndpoints.verifyOtp,
      method: 'POST',
      body: {'email': email.trim(), 'code': code.trim()},
    );
    if (data is Map && data['token'] != null && data['user'] != null) {
      final user = Profile.fromJson(
          Map<String, dynamic>.from(data['user'] as Map<String, dynamic>));
      await _saveSession(data['token'].toString(), user);
      return user;
    }
    throw ApiException('رمز التحقق غير صحيح');
  }

  Future<void> resendOtp(String email) async {
    if (useMockApi) return;
    await ApiClient.instance.request(
      AppEndpoints.resendOtp,
      method: 'POST',
      body: {'email': email.trim()},
    );
  }

  Future<Profile> login({required String email, required String password}) async {
    if (useMockApi) {
      await Future.delayed(const Duration(milliseconds: mockLatencyMs));
      final user = _mockProfile(email);
      await _saveSession('mock-$email', user);
      return user;
    }
    final data = await ApiClient.instance.request(
      AppEndpoints.login,
      method: 'POST',
      body: {'email': email.trim(), 'password': password},
    );
    if (data is Map && data['token'] != null && data['user'] != null) {
      final user = Profile.fromJson(
          Map<String, dynamic>.from(data['user'] as Map<String, dynamic>));
      await _saveSession(data['token'].toString(), user);
      return user;
    }
    throw ApiException('بيانات الدخول غير صحيحة');
  }

  Future<Profile> fetchProfile() async {
    final data = await ApiClient.instance.request(
      AppEndpoints.profile,
      auth: true,
    );
    final user = Profile.fromJson(
        Map<String, dynamic>.from((data is Map && data['user'] is Map)
            ? data['user'] as Map<String, dynamic>
            : data as Map<String, dynamic>));
    await _saveSession(ApiClient.instance.token, user);
    return user;
  }

  Future<Profile> updatePhone(String phone) async {
    await ApiClient.instance.request(
      AppEndpoints.profile,
      method: 'PUT',
      auth: true,
      body: {'phone_number': phone.replaceAll(' ', '')},
    );
    final u = _profile;
    if (u != null) {
      final updated = u.copyWith(phone: phone.replaceAll(' ', ''));
      await _saveSession(ApiClient.instance.token, updated);
      return updated;
    }
    throw ApiException('غير مسجل الدخول');
  }

  Future<void> logout() async {
    if (!useMockApi && isLoggedIn) {
      try {
        await ApiClient.instance.request(
          AppEndpoints.logout,
          method: 'POST',
          auth: true,
        );
      } catch (_) {
        // ignore network errors on logout
      }
    }
    ApiClient.instance.token = null;
    _profile = null;
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_sessionKey);
  }

  // ---------------- mock helpers (mirror web demo user) ----------------

  Profile _mockProfile(String email) => Profile(
        id: 1,
        fullName: email == 'demo@example.com' ? 'Demo User' : 'مستخدم VELOX',
        email: email,
        phone: '01012345678',
        governorate: 'DAMIETTA',
      );

  Future<Map<String, dynamic>> _mockRegister(String email, String password,
      String fullName, String phone, String governorate) async {
    await Future.delayed(const Duration(milliseconds: mockLatencyMs));
    if (email.toLowerCase() == 'demo@example.com') {
      throw ApiException('البريد مستخدم من قبل',
          statusCode: 400, data: {'message': 'EMAIL_ALREADY_REGISTERED'});
    }
    return {'pending': true, 'email': email, 'otp': '123456'};
  }
}