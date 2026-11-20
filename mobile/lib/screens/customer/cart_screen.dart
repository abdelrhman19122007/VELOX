import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/app_state.dart';
import '../../core/models.dart';
import '../../widgets/common.dart';
import 'checkout_screen.dart';

/// The cart: items with notes and quantities, a promo code, and the server's price (never the app's own sum).
class CartScreen extends StatefulWidget {
  const CartScreen({super.key});
  @override
  State<CartScreen> createState() => _CartScreenState();
}

class _CartScreenState extends State<CartScreen> {
  Quote? quote;
  Object? quoteError;
  late TextEditingController promo;
  String _lastKey = '';

  @override
  void initState() {
    super.initState();
    promo = TextEditingController(text: context.read<AppState>().promo);
  }

  /// Re-prices when the cart, area or code changes.
  void _maybeQuote(AppState s) {
    final key = '${s.cart.values.map((c) => '${c.product.id}x${c.qty}').join(',')}|${s.zone}|${s.promo}';
    if (key == _lastKey || s.cart.isEmpty) return;
    _lastKey = key;
    s.quote().then((q) {
      if (mounted) setState(() { quote = q; quoteError = null; });
    }).catchError((e) {
      if (mounted) setState(() { quote = null; quoteError = e; });
    });
  }

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    if (s.cart.isEmpty) {
      _lastKey = '';
      return Scaffold(
        appBar: AppBar(title: Text(s.t('cart'))),
        body: EmptyState(Icons.shopping_bag_outlined, s.t('cartEmpty'),
            subtitle: s.t('cartEmptySub'),
            action: FilledButton(onPressed: () => context.read<AppState>().goTab(0), child: Text(s.t('browse')))),
      );
    }
    WidgetsBinding.instance.addPostFrameCallback((_) => _maybeQuote(s));
    final promoError = quoteError is Object && s.errorText(quoteError!) == s.t('err_INVALID_PROMO');
    return Scaffold(
      appBar: AppBar(title: Text('${s.t('cart')} · ${s.cartStoreName ?? ''}')),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        for (final line in s.cart.values) _line(s, cs, line),
        const SizedBox(height: 12),
        Row(children: [
          Expanded(
            child: TextField(
              controller: promo,
              textCapitalization: TextCapitalization.characters,
              decoration: InputDecoration(
                labelText: s.t('promo'),
                prefixIcon: const Icon(Icons.local_offer_outlined),
                errorText: promoError ? s.t('err_INVALID_PROMO') : null,
              ),
            ),
          ),
          const SizedBox(width: 8),
          OutlinedButton(
            onPressed: () {
              FocusScope.of(context).unfocus();
              s.setPromo(promo.text);
            },
            child: Text(s.t('apply')),
          ),
        ]),
        if (s.promo.isNotEmpty && promoError)
          TextButton(
            onPressed: () {
              promo.clear();
              s.setPromo('');
            },
            child: Text('${s.t('cancel')} ${s.promo}'),
          ),
        const SizedBox(height: 16),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: quote == null
                ? (quoteError != null && !promoError
                    ? Text(s.errorText(quoteError!), style: TextStyle(color: cs.error))
                    : const Center(child: Padding(padding: EdgeInsets.all(8), child: CircularProgressIndicator())))
                : Column(children: [
                    PriceRow(s.t('subtotal'), s.money(quote!.subtotal)),
                    PriceRow('${s.t('deliveryFee')} (${s.zoneName(s.zone)})',
                        quote!.deliveryFee == 0 ? s.t('freeDelivery') : s.money(quote!.deliveryFee)),
                    if (quote!.discount > 0) PriceRow(s.t('discount'), '− ${s.money(quote!.discount)}', color: cs.secondary),
                    const Divider(height: 18),
                    PriceRow(s.t('total'), s.money(quote!.total), bold: true),
                    const SizedBox(height: 6),
                    Row(children: [
                      Icon(Icons.schedule, size: 18, color: cs.onSurfaceVariant),
                      const SizedBox(width: 6),
                      Text('${s.t('eta')} ${quote!.etaMinutes} ${s.t('min')}', style: TextStyle(color: cs.onSurfaceVariant)),
                    ]),
                  ]),
          ),
        ),
        const SizedBox(height: 90),
      ]),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 4, 16, 10),
          child: FilledButton(
            onPressed: quote == null
                ? null
                : () => Navigator.push(context, MaterialPageRoute(builder: (_) => CheckoutScreen(quote: quote!))),
            child: Text('${s.t('checkout')}${quote == null ? '' : ' · ${s.money(quote!.total)}'}'),
          ),
        ),
      ),
    );
  }

  Widget _line(AppState s, ColorScheme cs, CartLine line) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(10),
          child: Row(children: [
            ProductImage(line.product.photoUrl, size: 60),
            const SizedBox(width: 10),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(s.productName(line.product), style: const TextStyle(fontWeight: FontWeight.w800)),
                Text(s.money(line.product.price * line.qty), style: TextStyle(color: cs.primary, fontWeight: FontWeight.w700)),
                InkWell(
                  onTap: () => _editNote(s, line),
                  child: Text(line.note.isEmpty ? '+ ${s.t('note')}' : '“${line.note}”',
                      style: TextStyle(color: cs.onSurfaceVariant, fontSize: 13)),
                ),
              ]),
            ),
            IconButton(onPressed: () => s.setQty(line.product.id, line.qty - 1), icon: Icon(line.qty == 1 ? Icons.delete_outline : Icons.remove)),
            Text('${line.qty}', style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 16)),
            IconButton(onPressed: () => s.setQty(line.product.id, line.qty + 1), icon: const Icon(Icons.add)),
          ]),
        ),
      ),
    );
  }

  Future<void> _editNote(AppState s, CartLine line) async {
    final c = TextEditingController(text: line.note);
    final ok = await showDialog<bool>(
      context: context,
      builder: (d) => AlertDialog(
        content: TextField(controller: c, maxLength: 200, autofocus: true, decoration: InputDecoration(labelText: s.t('note'))),
        actions: [
          TextButton(onPressed: () => Navigator.pop(d, false), child: Text(s.t('cancel'))),
          FilledButton(onPressed: () => Navigator.pop(d, true), child: Text(s.t('save'))),
        ],
      ),
    );
    if (ok == true) s.setNote(line.product.id, c.text.trim());
  }
}
