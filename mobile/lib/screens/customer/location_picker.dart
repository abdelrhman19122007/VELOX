import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:geolocator/geolocator.dart';
import 'package:latlong2/latlong.dart';
import 'package:provider/provider.dart';

import '../../core/app_state.dart';
import '../../widgets/common.dart';

/// Samannoud's town centre — where the map opens when we know nothing better.
const samannoud = LatLng(30.9617, 31.2440);

/// Pick the exact delivery spot: drag the map under the pin, or jump to the phone's GPS position.
/// Returns the chosen point, or null when the customer goes back.
class LocationPicker extends StatefulWidget {
  final LatLng? start;
  const LocationPicker({super.key, this.start});
  @override
  State<LocationPicker> createState() => _LocationPickerState();
}

class _LocationPickerState extends State<LocationPicker> {
  final map = MapController();
  late LatLng center = widget.start ?? samannoud;
  bool locating = false;

  Future<void> _myLocation(AppState s) async {
    setState(() => locating = true);
    try {
      if (!await Geolocator.isLocationServiceEnabled()) throw s.t('locationOff');
      var perm = await Geolocator.checkPermission();
      if (perm == LocationPermission.denied) perm = await Geolocator.requestPermission();
      if (perm == LocationPermission.denied || perm == LocationPermission.deniedForever) throw s.t('locationDenied');
      final p = await Geolocator.getCurrentPosition(locationSettings: const LocationSettings(accuracy: LocationAccuracy.high));
      center = LatLng(p.latitude, p.longitude);
      map.move(center, 18);
    } catch (e) {
      if (mounted) showInfo(context, e is String ? e : s.t('locationDenied'));
    } finally {
      if (mounted) setState(() => locating = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: Text(s.t('pinTitle'))),
      body: Stack(children: [
        FlutterMap(
          mapController: map,
          options: MapOptions(
            initialCenter: center,
            initialZoom: widget.start == null ? 15 : 18,
            onPositionChanged: (pos, _) => center = pos.center,
          ),
          children: [
            TileLayer(urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png', userAgentPackageName: 'com.velox.velox_app'),
            const RichAttributionWidget(attributions: [TextSourceAttribution('OpenStreetMap contributors')]),
          ],
        ),
        // the pin stays in the middle; the map moves under it
        IgnorePointer(
          child: Center(
            child: Padding(
              padding: const EdgeInsets.only(bottom: 44),
              child: Icon(Icons.location_on, size: 52, color: cs.primary, shadows: const [Shadow(blurRadius: 6, color: Colors.black38)]),
            ),
          ),
        ),
        Positioned(
          top: 12,
          left: 12,
          right: 12,
          child: Card(
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Text(s.t('pinHint'), textAlign: TextAlign.center, style: const TextStyle(fontWeight: FontWeight.w700)),
            ),
          ),
        ),
      ]),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
          child: Row(children: [
            Expanded(
              child: OutlinedButton.icon(
                onPressed: locating ? null : () => _myLocation(s),
                icon: locating
                    ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                    : const Icon(Icons.my_location),
                label: Text(s.t('myLocation')),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: FilledButton.icon(
                onPressed: () => Navigator.pop(context, center),
                icon: const Icon(Icons.check),
                label: Text(s.t('pinConfirm')),
              ),
            ),
          ]),
        ),
      ),
    );
  }
}

/// A small, non-interactive map with the chosen pin — shown on checkout once a spot is picked.
class PinPreview extends StatelessWidget {
  final LatLng point;
  const PinPreview(this.point, {super.key});
  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return ClipRRect(
      borderRadius: BorderRadius.circular(14),
      child: SizedBox(
        height: 140,
        child: IgnorePointer(
          child: FlutterMap(
            options: MapOptions(initialCenter: point, initialZoom: 17),
            children: [
              TileLayer(urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png', userAgentPackageName: 'com.velox.velox_app'),
              MarkerLayer(markers: [
                Marker(point: point, width: 44, height: 44, alignment: Alignment.topCenter, child: Icon(Icons.location_on, size: 44, color: cs.primary)),
              ]),
            ],
          ),
        ),
      ),
    );
  }
}
