import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';

import 'api.dart';

enum PushState { unavailable, off, on, blocked }

/// Push notifications (Firebase Cloud Messaging). The Firebase settings come from the server (GET /api/push-config,
/// {} when push is off), so the app needs no rebuild when they change and a server without them simply has no push.
class Push {
  static String? _token;
  static PushState state = PushState.unavailable;
  static Map<String, dynamic>? _config;

  /// Registers this device for the signed-in account. With [ask] false it only does so when the user already
  /// allowed notifications (browsers want the question to come from a button press).
  static Future<PushState> enable(Api api, String lang, {bool ask = false}) async {
    try {
      _config ??= Map<String, dynamic>.from(await api.get('/push-config') ?? {});
      final c = _config!;
      if (c['apiKey'] == null || c['appId'] == null) return state = PushState.unavailable;
      if (Firebase.apps.isEmpty) {
        await Firebase.initializeApp(
          options: FirebaseOptions(
            apiKey: c['apiKey'],
            appId: (!kIsWeb && defaultTargetPlatform == TargetPlatform.android ? c['androidAppId'] : null) ?? c['appId'],
            messagingSenderId: c['messagingSenderId'] ?? '',
            projectId: c['projectId'] ?? '',
            authDomain: c['authDomain'],
            storageBucket: c['storageBucket'],
          ),
        );
      }
      final m = FirebaseMessaging.instance;
      var settings = await m.getNotificationSettings();
      if (settings.authorizationStatus != AuthorizationStatus.authorized && ask) {
        settings = await m.requestPermission();
      }
      switch (settings.authorizationStatus) {
        case AuthorizationStatus.denied:
          return state = PushState.blocked;
        case AuthorizationStatus.authorized:
        case AuthorizationStatus.provisional:
          break;
        default:
          return state = PushState.off;
      }
      final token = await m.getToken(vapidKey: kIsWeb ? c['vapidKey'] : null);
      if (token == null) return state = PushState.off;
      await _register(api, token, lang);
      m.onTokenRefresh.listen((t) => _register(api, t, lang));
      return state = PushState.on;
    } catch (e) {
      debugPrint('push: $e');
      return state = (_config == null || _config!.isEmpty) ? PushState.unavailable : PushState.off;
    }
  }

  static Future<void> _register(Api api, String token, String lang) async {
    _token = token;
    await api.post('/me/devices', {
      'token': token,
      'platform': kIsWeb ? 'web' : (defaultTargetPlatform == TargetPlatform.iOS ? 'ios' : 'android'),
      'lang': lang,
    });
  }

  /// Signing out: this device stops getting the account's notifications. Call before the token is cleared.
  static void disable(Api api) {
    final t = _token;
    _token = null;
    if (t != null && api.token != null) api.post('/me/devices/remove', {'token': t}).catchError((_) => null);
    if (state == PushState.on) state = PushState.off;
  }
}
