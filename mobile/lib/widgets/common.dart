import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';

import '../core/api.dart';
import '../core/app_state.dart';
import '../core/models.dart';

/// Shows a server error (translated) at the bottom of the screen.
void showError(BuildContext context, Object e) {
  final s = context.read<AppState>();
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(content: Text(s.errorText(e)), backgroundColor: Theme.of(context).colorScheme.error));
}

void showInfo(BuildContext context, String text) {
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(content: Text(text)));
}

/// Runs [action] with a spinner on the button that started it; shows errors.
Future<T?> busy<T>(BuildContext context, Future<T> Function() action) async {
  try {
    return await action();
  } catch (e) {
    if (context.mounted) showError(context, e);
    return null;
  }
}

Future<bool> confirmDialog(BuildContext context, String text) async {
  final s = context.read<AppState>();
  return await showDialog<bool>(
        context: context,
        builder: (c) => AlertDialog(
          content: Text(text),
          actions: [
            TextButton(onPressed: () => Navigator.pop(c, false), child: Text(s.t('no'))),
            FilledButton(onPressed: () => Navigator.pop(c, true), child: Text(s.t('yes'))),
          ],
        ),
      ) ??
      false;
}

void callPhone(String phone) => launchUrl(Uri.parse('tel:$phone'));

String timeText(BuildContext context, DateTime? t) {
  if (t == null) return '';
  final s = context.read<AppState>();
  return DateFormat('d MMM · h:mm a', s.lang).format(t);
}

/// A store's picture, or — when it has none — its type icon on a warm gradient (never a fake photo).
/// Photos fade in when they arrive instead of popping in.
Widget _fadeIn(BuildContext context, Widget child, int? frame, bool sync) => sync
    ? child
    : AnimatedOpacity(opacity: frame == null ? 0 : 1, duration: const Duration(milliseconds: 350), curve: Curves.easeOut, child: child);

/// List items rise and fade in one after another when a list first shows.
class FadeSlideIn extends StatelessWidget {
  final int index;
  final Widget child;
  const FadeSlideIn({super.key, required this.index, required this.child});
  @override
  Widget build(BuildContext context) {
    final delay = index.clamp(0, 8) * 60;
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0, end: 1),
      duration: Duration(milliseconds: 380 + delay),
      curve: Interval(delay / (380 + delay), 1, curve: Curves.easeOutCubic),
      builder: (c, v, child) => Opacity(opacity: v, child: Transform.translate(offset: Offset(0, 24 * (1 - v)), child: child)),
      child: child,
    );
  }
}

class StoreImage extends StatelessWidget {
  final String? photoUrl;
  final String type;
  final double height;
  final BorderRadius radius;
  const StoreImage({super.key, this.photoUrl, required this.type, this.height = 140, this.radius = BorderRadius.zero});

  static IconData iconFor(String type) => switch (type) {
        'FOOD' => Icons.restaurant,
        'GROCERY' => Icons.local_grocery_store,
        'PHARMACY' => Icons.local_pharmacy,
        'CLOTHES' => Icons.checkroom,
        'TECH' => Icons.devices,
        _ => Icons.storefront,
      };

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final url = imageUrl(photoUrl);
    final placeholder = Container(
      height: height,
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [cs.primaryContainer, cs.surfaceContainerHigh],
          begin: Alignment.topRight,
          end: Alignment.bottomLeft,
        ),
      ),
      child: Center(child: Icon(iconFor(type), size: height * 0.38, color: cs.primary.withValues(alpha: 0.75))),
    );
    return ClipRRect(
      borderRadius: radius,
      child: url == null
          ? placeholder
          : Image.network(url,
              height: height,
              width: double.infinity,
              fit: BoxFit.cover,
              errorBuilder: (_, _, _) => placeholder,
              frameBuilder: _fadeIn),
    );
  }
}

class ProductImage extends StatelessWidget {
  final String? photoUrl;
  final double size;
  const ProductImage(this.photoUrl, {super.key, this.size = 84});
  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final url = imageUrl(photoUrl);
    final box = Container(
      width: size,
      height: size,
      color: cs.surfaceContainerHigh,
      child: Icon(Icons.fastfood_outlined, color: cs.onSurfaceVariant),
    );
    return ClipRRect(
      borderRadius: BorderRadius.circular(14),
      child: url == null
          ? box
          : Image.network(url, width: size, height: size, fit: BoxFit.cover, errorBuilder: (_, _, _) => box, frameBuilder: _fadeIn),
    );
  }
}

/// Order status with one colour per state, used by every role.
class StatusChip extends StatelessWidget {
  final String status;
  const StatusChip(this.status, {super.key});

  static Color colorFor(BuildContext context, String status) {
    final cs = Theme.of(context).colorScheme;
    return switch (status) {
      'PENDING_PAYMENT' => cs.tertiary,
      'PLACED' => cs.primary,
      'ACCEPTED' || 'READY' => const Color(0xFF2F6FB0),
      'PICKED_UP' => const Color(0xFF7A4CC2),
      'DELIVERED' => cs.secondary,
      _ => cs.error,
    };
  }

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final c = colorFor(context, status);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(color: c.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(20)),
      child: Text(s.t('st_$status'), style: TextStyle(color: c, fontWeight: FontWeight.w700, fontSize: 13)),
    );
  }
}

class EmptyState extends StatelessWidget {
  final IconData icon;
  final String title;
  final String? subtitle;
  final Widget? action;
  const EmptyState(this.icon, this.title, {super.key, this.subtitle, this.action});
  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          CircleAvatar(radius: 40, backgroundColor: cs.primaryContainer, child: Icon(icon, size: 38, color: cs.primary)),
          const SizedBox(height: 16),
          Text(title, textAlign: TextAlign.center, style: tt.titleMedium?.copyWith(fontWeight: FontWeight.w800)),
          if (subtitle != null) ...[
            const SizedBox(height: 6),
            Text(subtitle!, textAlign: TextAlign.center, style: tt.bodyMedium?.copyWith(color: cs.onSurfaceVariant)),
          ],
          if (action != null) ...[const SizedBox(height: 18), action!],
        ]),
      ),
    );
  }
}

class Stars extends StatelessWidget {
  final double? rating;
  final int count;
  const Stars(this.rating, this.count, {super.key});
  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    if (rating == null || count == 0) {
      return Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
        decoration: BoxDecoration(color: cs.secondaryContainer, borderRadius: BorderRadius.circular(8)),
        child: Text(s.t('newStore'), style: TextStyle(color: cs.onSecondaryContainer, fontSize: 12, fontWeight: FontWeight.w700)),
      );
    }
    return Row(mainAxisSize: MainAxisSize.min, children: [
      const Icon(Icons.star_rounded, color: Color(0xFFE8A317), size: 18),
      const SizedBox(width: 2),
      Text(rating!.toStringAsFixed(1), style: const TextStyle(fontWeight: FontWeight.w800)),
      Text(' ($count)', style: TextStyle(color: cs.onSurfaceVariant, fontSize: 12.5)),
    ]);
  }
}

class StarPicker extends StatelessWidget {
  final int value;
  final ValueChanged<int> onChanged;
  const StarPicker(this.value, this.onChanged, {super.key});
  @override
  Widget build(BuildContext context) => Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          for (var i = 1; i <= 5; i++)
            IconButton(
              visualDensity: VisualDensity.compact,
              onPressed: () => onChanged(i),
              icon: Icon(i <= value ? Icons.star_rounded : Icons.star_outline_rounded, color: const Color(0xFFE8A317), size: 34),
            ),
        ],
      );
}

/// A label / amount row in price breakdowns.
class PriceRow extends StatelessWidget {
  final String label;
  final String value;
  final bool bold;
  final Color? color;
  const PriceRow(this.label, this.value, {super.key, this.bold = false, this.color});
  @override
  Widget build(BuildContext context) {
    final style = TextStyle(fontWeight: bold ? FontWeight.w800 : FontWeight.w500, fontSize: bold ? 17 : 15, color: color);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(children: [Expanded(child: Text(label, style: style)), Text(value, style: style)]),
    );
  }
}

class SectionTitle extends StatelessWidget {
  final String text;
  final Widget? trailing;
  const SectionTitle(this.text, {super.key, this.trailing});
  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.fromLTRB(16, 20, 16, 10),
        child: Row(children: [
          Expanded(child: Text(text, style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800))),
          ?trailing,
        ]),
      );
}

/// Items of an order, with notes.
class OrderItemsList extends StatelessWidget {
  final OrderView order;
  const OrderItemsList(this.order, {super.key});
  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    return Column(children: [
      for (final i in order.items)
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 4),
          child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text('${i.qty}×  ', style: TextStyle(fontWeight: FontWeight.w800, color: cs.primary)),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(s.itemName(i)),
                if ((i.note ?? '').isNotEmpty) Text('“${i.note}”', style: TextStyle(color: cs.onSurfaceVariant, fontSize: 13)),
              ]),
            ),
            Text(s.money(i.lineTotal)),
          ]),
        ),
    ]);
  }
}

/// Address block with the landmark highlighted (that's how people in Samannoud find a house).
class AddressBlock extends StatelessWidget {
  final OrderView order;
  final bool showPhone;
  const AddressBlock(this.order, {super.key, this.showPhone = false});
  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Row(children: [
        Icon(Icons.location_on, color: cs.primary, size: 20),
        const SizedBox(width: 6),
        Expanded(child: Text('${s.zoneName(order.zone)} — ${order.address}')),
      ]),
      if ((order.landmark ?? '').isNotEmpty)
        Padding(
          padding: const EdgeInsetsDirectional.only(start: 26, top: 4),
          child: Text('🏁 ${order.landmark}', style: TextStyle(color: cs.secondary, fontWeight: FontWeight.w700)),
        ),
      if (showPhone)
        Padding(
          padding: const EdgeInsetsDirectional.only(start: 20, top: 2),
          child: TextButton.icon(onPressed: () => callPhone(order.phone), icon: const Icon(Icons.call), label: Text(order.phone)),
        ),
    ]);
  }
}
