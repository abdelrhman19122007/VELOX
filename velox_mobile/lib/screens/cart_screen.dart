import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/locale.dart';
import '../services/cart_state.dart';
import '../services/checkout_logic.dart';
import '../theme/app_theme.dart';
import '../widgets/empty_state.dart';
import '../widgets/glass_card.dart';
import '../widgets/primary_button.dart';
import 'browse_screen.dart';
import 'checkout_screen.dart';

class CartScreen extends StatelessWidget {
  const CartScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(AppLocale.tr('app.cart'))),
      body: Consumer<CartState>(
        builder: (_, cart, __) {
          if (cart.isEmpty) {
            return EmptyState(
              icon: Icons.shopping_bag_outlined,
              title: AppLocale.tr('cart.empty'),
              hint: AppLocale.tr('cart.emptyHint'),
              actionLabel: AppLocale.tr('cart.browse'),
              onAction: () {
                Navigator.of(context).push(
                  MaterialPageRoute(
                      builder: (_) => const BrowseScreen(initial: 'all')),
                );
              },
            );
          }
          final govId = 'CAIRO'; // server prices the governorate fee
          final shipping = CheckoutData.byId(govId).shippingFee;
          final subtotal = cart.subtotal;
          final total = subtotal + shipping;

          return Column(
            children: [
              Expanded(
                child: ListView.separated(
                  padding: const EdgeInsets.all(16),
                  itemCount: cart.items.length,
                  separatorBuilder: (_, __) => const SizedBox(height: 10),
                  itemBuilder: (_, i) {
                    final item = cart.items[i];
                    return GlassCard(
                      padding: const EdgeInsets.all(10),
                      child: Row(
                        children: [
                          ClipRRect(
                            borderRadius: BorderRadius.circular(10),
                            child: SizedBox(
                              width: 64,
                              height: 64,
                              child: Image.asset(
                                item.product.imageSource(),
                                fit: BoxFit.cover,
                                errorBuilder: (_, __, ___) => Container(
                                  color: AppColors.surfaceHigh,
                                  child: const Icon(Icons.image,
                                      color: AppColors.textDisabled),
                                ),
                              ),
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  item.product.nameFor(AppLocale.lang),
                                  maxLines: 2,
                                  overflow: TextOverflow.ellipsis,
                                  style: const TextStyle(
                                      fontSize: 13,
                                      fontWeight: FontWeight.w600),
                                ),
                                const SizedBox(height: 4),
                                Text(
                                  AppLocale.money(item.product.price),
                                  style: const TextStyle(
                                      color: AppColors.primary,
                                      fontWeight: FontWeight.w700,
                                      fontSize: 13),
                                ),
                              ],
                            ),
                          ),
                          _RowQty(
                            qty: item.qty,
                            onMinus: () => cart.setQty(item.product, item.qty - 1),
                            onPlus: () => cart.setQty(item.product, item.qty + 1),
                          ),
                        ],
                      ),
                    );
                  },
                ),
              ),
              // Sticky totals tray (Tier 2 glass, pinned bottom)
              Container(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 20),
                decoration: const BoxDecoration(
                  color: AppColors.surfaceLow,
                  border: Border(top: BorderSide(color: AppColors.border)),
                ),
                child: Column(
                  children: [
                    _totalRow(AppLocale.tr('cart.subtotal'),
                        AppLocale.money(subtotal)),
                    _totalRow(AppLocale.tr('cart.shipping'),
                        AppLocale.money(shipping)),
                    const Divider(),
                    _totalRow(
                      AppLocale.tr('cart.total'),
                      AppLocale.money(total),
                      bold: true,
                    ),
                    const SizedBox(height: 10),
                    PrimaryButton(
                      label: AppLocale.tr('cart.checkout'),
                      icon: Icons.arrow_forward,
                      onPressed: () {
                        Navigator.of(context).push(MaterialPageRoute(
                            builder: (_) => const CheckoutScreen()));
                      },
                    ),
                  ],
                ),
              ),
            ],
          );
        },
      ),
    );
  }

  Widget _totalRow(String label, String value, {bool bold = false}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        children: [
          Text(label,
              style: TextStyle(
                  fontSize: bold ? 15 : 13,
                  fontWeight: bold ? FontWeight.w800 : FontWeight.w500,
                  color: bold ? AppColors.textHigh : AppColors.textMuted)),
          const Spacer(),
          Text(value,
              style: TextStyle(
                  fontSize: bold ? 16 : 13,
                  fontWeight: bold ? FontWeight.w800 : FontWeight.w600,
                  color: AppColors.textHigh)),
        ],
      ),
    );
  }
}

class _RowQty extends StatelessWidget {
  final int qty;
  final VoidCallback onMinus;
  final VoidCallback onPlus;

  const _RowQty(
      {required this.qty, required this.onMinus, required this.onPlus});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.surfaceHigh,
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: AppColors.border),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          IconButton(
              onPressed: onMinus,
              icon: const Icon(Icons.remove, size: 16, color: AppColors.primary),
              visualDensity: VisualDensity.compact),
          Text('$qty', style: const TextStyle(fontWeight: FontWeight.w800)),
          IconButton(
              onPressed: onPlus,
              icon: const Icon(Icons.add, size: 16, color: AppColors.primary),
              visualDensity: VisualDensity.compact),
        ],
      ),
    );
  }
}