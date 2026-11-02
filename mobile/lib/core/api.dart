import 'dart:async';
import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

/// Where the VELOX server is.
/// 1. A link typed in the app (Settings → Server) wins, so a phone keeps working when the public link changes.
/// 2. Then a build-time value: flutter build apk --dart-define=API_URL=https://my-server
/// 3. In a browser served by the server itself, the same address.
/// 4. Otherwise the PC as seen from the Android emulator (10.0.2.2) or localhost.
String? serverOverride;

String get apiBase {
  if (serverOverride != null && serverOverride!.isNotEmpty) return serverOverride!;
  const fromEnv = String.fromEnvironment('API_URL');
  if (fromEnv.isNotEmpty) return fromEnv;
  if (kIsWeb) {
    final here = Uri.base;
    final devServer = (here.host == 'localhost' || here.host == '127.0.0.1') && here.port != 8080;
    return devServer ? 'http://localhost:8080' : here.origin;
  }
  if (defaultTargetPlatform == TargetPlatform.android) return 'http://10.0.2.2:8080';
  return 'http://localhost:8080';
}

/// Where the PC running VELOX announces its current public link (free ntfy.sh topic, no account).
/// deploy/start-public.ps1 posts every new tunnel link here; the app reads the newest one when its saved link
/// stops answering, so a new link never needs typing.
const discoveryTopic = 'velox-nabeeh-k7q2m9';

/// The newest announced server link that actually answers, or null.
Future<String?> discoverServer() async {
  try {
    final res = await http
        .get(Uri.parse('https://ntfy.sh/$discoveryTopic/json?poll=1&since=12h'))
        .timeout(const Duration(seconds: 10));
    if (res.statusCode != 200) return null;
    String? latest;
    for (final line in const LineSplitter().convert(utf8.decode(res.bodyBytes))) {
      if (line.trim().isEmpty) continue;
      final m = jsonDecode(line);
      final msg = '${m['message'] ?? ''}'.trim();
      if (m['event'] == 'message' && RegExp(r'^https://[\w.-]+$').hasMatch(msg)) latest = msg;
    }
    if (latest == null) return null;
    final check = await http.get(Uri.parse('$latest/api/zones')).timeout(const Duration(seconds: 10));
    return check.statusCode == 200 ? latest : null;
  } catch (_) {
    return null;
  }
}

/// Photos are stored on the server ("/uploads/…") or are full links.
String? imageUrl(String? path) {
  if (path == null || path.isEmpty) return null;
  if (path.startsWith('http')) return path;
  return '$apiBase${path.startsWith('/') ? '' : '/'}$path';
}

/// An error from the server: a stable [code] the app translates (ORDER_NOT_FOUND, WRONG_CODE…).
class ApiError implements Exception {
  final String code;
  final String message;
  final Map<String, dynamic> details;
  ApiError(this.code, this.message, [this.details = const {}]);
  @override
  String toString() => '$code: $message';
}

class Api {
  String? token;
  final http.Client _http = http.Client();

  /// Called when the server says the session is no longer valid (signed out elsewhere, password changed, blocked).
  void Function()? onSessionEnded;

  Map<String, String> get _headers => {
        'Content-Type': 'application/json',
        if (token != null) 'Authorization': 'Bearer $token',
      };

  Uri _uri(String path) => Uri.parse('$apiBase/api$path');

  Future<dynamic> get(String path) => _send(() => _http.get(_uri(path), headers: _headers));
  Future<dynamic> post(String path, [Object? body]) =>
      _send(() => _http.post(_uri(path), headers: _headers, body: body == null ? null : jsonEncode(body)));
  Future<dynamic> put(String path, Object body) =>
      _send(() => _http.put(_uri(path), headers: _headers, body: jsonEncode(body)));
  Future<dynamic> patch(String path, Object body) =>
      _send(() => _http.patch(_uri(path), headers: _headers, body: jsonEncode(body)));
  Future<dynamic> delete(String path) => _send(() => _http.delete(_uri(path), headers: _headers));

  Future<dynamic> _send(Future<http.Response> Function() call) async {
    http.Response res;
    try {
      res = await call().timeout(const Duration(seconds: 45));
    } catch (_) {
      throw ApiError('NETWORK', 'Can’t reach the server');
    }
    final text = utf8.decode(res.bodyBytes);
    dynamic data;
    try {
      data = text.isEmpty ? null : jsonDecode(text);
    } catch (_) {
      data = null;
    }
    if (res.statusCode >= 400) {
      if (res.statusCode == 401 && token != null) onSessionEnded?.call();
      if (data is Map && data['code'] != null) {
        throw ApiError('${data['code']}', '${data['message'] ?? ''}', Map<String, dynamic>.from(data['details'] ?? {}));
      }
      throw ApiError('SERVER', 'HTTP ${res.statusCode}');
    }
    return data;
  }

  /// Live updates (Server-Sent Events from /api/events). Reconnects by itself until [LiveStream.close]d.
  LiveStream live(void Function(String event, dynamic data) onEvent) => LiveStream(this, onEvent);
}

/// Live updates: a Server-Sent Events stream, or — when something on the way holds the stream back
/// (Cloudflare quick tunnels, some mobile networks) — long polling of /api/events/poll.
class LiveStream {
  final Api api;
  final void Function(String event, dynamic data) onEvent;
  bool _closed = false;
  bool _polling = false;
  http.Client? _client;

  LiveStream(this.api, this.onEvent) {
    _run();
  }

  Future<void> _run() async {
    while (!_closed && api.token != null) {
      final hello = Completer<void>();
      final done = _stream(hello);
      // The server says "hello" at once; silence for a few seconds means the stream is being held back.
      final first = await Future.any([
        hello.future.then((_) => 'hello'),
        done.then((_) => 'ended'),
        Future.delayed(const Duration(seconds: 6), () => 'silent'),
      ]);
      if (first == 'silent' && !hello.isCompleted) {
        _polling = true;
        _client?.close();
        break;
      }
      if (first == 'hello') await done;
      if (!_closed) await Future.delayed(const Duration(seconds: 3));
    }
    if (_polling && !_closed) await _poll();
  }

  Future<void> _stream(Completer<void> hello) async {
    final client = _client = http.Client();
    try {
      final req = http.Request('GET', Uri.parse('$apiBase/api/events'))..headers['Authorization'] = 'Bearer ${api.token}';
      final res = await client.send(req);
      String? event;
      await for (final line in res.stream.transform(utf8.decoder).transform(const LineSplitter())) {
        if (_closed || _polling) break;
        if (line.startsWith('event:')) {
          event = line.substring(6).trim();
        } else if (line.startsWith('data:') && event != null) {
          if (event == 'hello' && !hello.isCompleted) hello.complete();
          try {
            onEvent(event, jsonDecode(line.substring(5)));
          } catch (_) {}
          event = null;
        }
      }
    } catch (_) {
      // connection dropped — the caller reconnects
    } finally {
      client.close();
    }
  }

  Future<void> _poll() async {
    int? after;
    while (!_closed && api.token != null) {
      _client = http.Client();
      try {
        final uri = Uri.parse('$apiBase/api/events/poll${after == null ? '' : '?after=$after'}');
        final res =
            await _client!.get(uri, headers: {'Authorization': 'Bearer ${api.token}'}).timeout(const Duration(seconds: 40));
        if (res.statusCode == 200) {
          final body = jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>;
          after = (body['last'] as num).toInt();
          for (final e in body['events'] as List) {
            if (_closed) break;
            onEvent(e['event'], e['data']);
          }
          continue;
        }
      } catch (_) {
        // network hiccup — retry below
      } finally {
        _client?.close();
      }
      if (!_closed) await Future.delayed(const Duration(seconds: 3));
    }
  }

  void close() {
    _closed = true;
    _client?.close();
  }
}
