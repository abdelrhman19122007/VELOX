import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import 'app_config.dart';

/// Thin HTTP wrapper around the VELOX Spring Boot backend.
/// Attaches the Bearer token when present and decodes JSON responses.
class ApiClient {
  ApiClient._();

  static final ApiClient instance = ApiClient._();

  String? _token;

  String get token => _token ?? '';

  set token(String? value) => _token = value;

  Uri _uri(String path) => Uri.parse('$apiBaseUrl$path');

  Map<String, String> _headers({bool json = true, bool auth = false}) {
    final h = {
      'Accept': 'application/json',
      if (json) 'Content-Type': 'application/json',
      if (auth && _token != null && _token!.isNotEmpty)
        'Authorization': 'Bearer $_token',
    };
    return h;
  }

  /// Throws [ApiException] with a friendly message on non-2xx responses
  /// or network failure, so callers can translate it for the UI.
  Future<dynamic> request(
    String path, {
    String method = 'GET',
    Map<String, dynamic>? body,
    bool auth = false,
    bool json = true,
  }) async {
    final uri = _uri(path);
    final headers = _headers(json: json, auth: auth);
    http.Response res;
    try {
      final encoded = body == null ? null : jsonEncode(body);
      switch (method) {
        case 'POST':
          res = await http.post(uri, headers: headers, body: encoded);
        case 'PUT':
          res = await http.put(uri, headers: headers, body: encoded);
        case 'PATCH':
          res = await http.patch(uri, headers: headers, body: encoded);
        case 'DELETE':
          res = await http.delete(uri, headers: headers);
        default:
          res = await http.get(uri, headers: headers);
      }
    } catch (_) {
      throw ApiException(
        'تعذر الاتصال بالخادم. تأكد من تشغيل الخادم ثم أعد المحاولة.',
        hint: 'Cannot reach backend at $apiBaseUrl',
      );
    }

    dynamic data;
    try {
      final text = res.body.isEmpty ? 'null' : res.body;
      data = jsonDecode(text);
    } catch (_) {
      data = null;
    }

    final ok = res.statusCode >= 200 && res.statusCode < 300;
    if (!ok) {
      String message = 'حدث خطأ (${res.statusCode})';
      if (data is Map && data['message'] != null) {
        message = data['message'].toString();
      }
      throw ApiException(message, statusCode: res.statusCode, data: data);
    }
    return data;
  }
}

class ApiException implements Exception {
  final String message;
  final int? statusCode;
  final String? hint;
  final dynamic data;

  ApiException(this.message, {this.statusCode, this.data, this.hint});

  @override
  String toString() => hint ?? message;
}