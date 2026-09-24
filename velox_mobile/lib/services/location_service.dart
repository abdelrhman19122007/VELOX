import 'dart:convert';

import 'package:geolocator/geolocator.dart';
import 'package:http/http.dart' as http;

import 'checkout_logic.dart';

/// Device-location → governorate detection (mirrors the web's
/// detectLocationAndFill): reverse geocode via Nominatim, then map the
/// city/region text against the 27 governorates.
class LocationService {
  LocationService._();

  static final LocationService instance = LocationService._();

  /// Returns the detected governorate id, or null when the user denies
  /// permission, times out, or the reverse lookup can't be mapped.
  Future<String?> detectGovernorate() async {
    try {
      final permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        final asked = await Geolocator.requestPermission();
        if (asked == LocationPermission.denied ||
            asked == LocationPermission.deniedForever) {
          return null;
        }
      }
      final pos = await Geolocator.getCurrentPosition(
        locationSettings: const LocationSettings(
          accuracy: LocationAccuracy.medium,
          timeLimit: Duration(seconds: 8),
        ),
      );
      final gov = await _reverseGeocode(pos.latitude, pos.longitude);
      return gov;
    } catch (_) {
      return null; // keep manual selection on any failure
    }
  }

  Future<String?> _reverseGeocode(double lat, double lon) async {
    try {
      final uri = Uri.parse(
          'https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=json&accept-language=ar');
      final res = await http.get(uri, headers: {'Accept': 'application/json'});
      if (res.statusCode != 200) return null;
      final j = jsonDecode(res.body) as Map<String, dynamic>;
      final a = (j['address'] as Map<String, dynamic>?) ?? {};
      final parts = [
        a['city'],
        a['town'],
        a['village'],
        a['hamlet'],
        a['state'],
        a['county'],
      ];
      final text = parts.whereType<String>().join(' ');
      final display = (j['display_name'] ?? '').toString();
      return CheckoutData.mapCityToGov(text) ??
          CheckoutData.mapCityToGov(display);
    } catch (_) {
      return null;
    }
  }
}