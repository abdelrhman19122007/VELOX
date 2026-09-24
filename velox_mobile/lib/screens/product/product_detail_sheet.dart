import 'package:flutter/material.dart';

import '../../core/locale.dart';
import '../../models/product.dart';
import '../../services/cart_state.dart';
import '../../services/checkout_logic.dart';
import '../../theme/app_theme.dart';
import '../../widgets/primary_button.dart';

/// Bottom-sheet product detail: runner, description, ETA chip,
/// quantity stepper + add-to-cart.
Future<void> showProductDetail(BuildContext context, Product p) {
  return showModalBottomSheet(
    context: context,
    isScrollControlled: true,
    backgroundColor: Colors.transparent,
    builder: (_) => _ProductDetailSheet(product: p),
  );
}

class _ProductDetailSheet extends StatefulWidget {
  final Product product;

  const _ProductDetailSheet({required this.product});

  @override
  State<_ProductDetailSheet> createState() => _ProductDetailSheetState();
}

class _ProductDetailSheetState extends State<_ProductDetailSheet> {
  int _qty = 1;

  @override
  Widget build(BuildContext context) {
    final p = widget.product;
    final lang = AppLocale.lang;
    final eta = CheckoutData.etaText('CAIRO', foodOnly: p.isFood);

    return Container(
      decoration: const BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      child: DraggableScrollableSheet(
        initialChildSize: .72,
        minChildSize: .5,
        maxChildSize: .95,
        expand: false,
        builder: (_, scrollCtrl) => ListView(
          controller: scrollCtrl,
          padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
          children: [
            Center(
              child: Container(
                width: 44,
                height: 4,
                decoration: BoxDecoration(
                  color: AppColors.borderStrong,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            const SizedBox(height: 16),
            ClipRRect(
              borderRadius: BorderRadius.circular(16),
              child: AspectRatio(
                aspectRatio: 1.35,
                child: Image.asset(
                  p.imageSource(),
                  fit: BoxFit.cover,
                  errorBuilder: (_, __, ___) => Container(
                    color: AppColors.surfaceHigh,
                    alignment: Alignment.center,
                    child: const Icon(Icons.image_not_supported_outlined,
                        color: AppColors.textDisabled, size: 44),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 16),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(p.nameFor(lang),
                          style: Theme.of(context).textTheme.headlineSmall),
                      const SizedBox(height: 6),
                      Row(
                        children: [
                          const Icon(Icons.storefront,
                              size: 14, color: AppColors.textMuted),
                          const SizedBox(width: 5),
                          Text(p.storeFor(lang),
                              style: const TextStyle(
                                  fontSize: 12, color: AppColors.textMuted)),
                        ],
                      ),
                    ],
                  ),
                ),
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                  decoration: BoxDecoration(
                    color: AppColors.secondary.withValues(alpha: .12),
                    borderRadius: BorderRadius.circular(999),
                    border: Border.all(color: AppColors.secondary),
                  ),
                  child: Text(
                    eta,
                    style: const TextStyle(
                        fontSize: 10,
                        fontWeight: FontWeight.w700,
                        color: AppColors.secondary),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Text(
              p.descFor(lang),
              style: const TextStyle(
                  fontSize: 13, color: AppColors.textMuted, height: 1.6),
            ),
            const SizedBox(height: 20),
            Row(
              children: [
                Text(
                  AppLocale.money(p.price),
                  style: const TextStyle(
                      fontSize: 22,
                      fontWeight: FontWeight.w800,
                      color: AppColors.textHigh),
                ),
                const Spacer(),
                _QtyStepper(
                  qty: _qty,
                  onChange: (v) => setState(() => _qty = v),
                ),
              ],
            ),
            const SizedBox(height: 20),
            PrimaryButton(
              label:
                  '${AppLocale.t('أضف للسلة', 'Add to cart')} · $_qty × ${AppLocale.money(p.price)}',
              icon: Icons.shopping_bag_outlined,
              onPressed: () {
                CartState.instance.add(p, qty: _qty);
                Navigator.of(context).pop();
                ScaffoldMessenger.of(context)
                  ..hideCurrentSnackBar()
                  ..showSnackBar(SnackBar(
                    content: Text(AppLocale.tr('pdt.added')),
                    duration: const Duration(seconds: 1),
                  ));
              },
            ),
          ],
        ),
      ),
    );
  }
}

class _QtyStepper extends StatelessWidget {
  final int qty;
  final ValueChanged<int> onChange;

  const _QtyStepper({required this.qty, required this.onChange});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.surfaceLow,
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: AppColors.border),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          _btn(Icons.remove, () => onChange(qty - 1)),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 14),
            child: Text('$qty',
                style: const TextStyle(
                    fontSize: 15, fontWeight: FontWeight.w800)),
          ),
          _btn(Icons.add, () => onChange(qty + 1)),
        ],
      ),
    );
  }

  Widget _btn(IconData icon, VoidCallback onTap) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(999),
      child: Padding(
        padding: const EdgeInsets.all(8),
        child: Icon(icon, size: 17, color: AppColors.primary),
      ),
    );
  }
}