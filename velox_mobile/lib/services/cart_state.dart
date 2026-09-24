import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../models/product.dart';
import 'catalog_service.dart';

class CartItem {
  final Product product;
  int qty;

  CartItem({required this.product, this.qty = 1});

  double get lineTotal => product.price * qty;
}

/// Persistent cart (ChangeNotifier).
class CartState extends ChangeNotifier {
  CartState._();

  static final CartState instance = CartState._();

  static const _key = 'velox_cart';

  final List<CartItem> _items = [];

  List<CartItem> get items => List.unmodifiable(_items);

  bool get isEmpty => _items.isEmpty;

  int get count => _items.fold(0, (s, i) => s + i.qty);

  double get subtotal => _items.fold(0.0, (s, i) => s + i.lineTotal);

  /// True when at least one cart line is food (drives ETA hours vs days).
  bool get isFoodOnly => _items.every((i) => i.product.isFood);

  Future<void> restore() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_key);
    if (raw == null) return;
    try {
      final data = jsonDecode(raw) as List;
      for (final e in data) {
        final m = Map<String, dynamic>.from(e as Map<String, dynamic>);
        final id = (m['id'] as num).toInt();
        final qty = (m['qty'] as num?)?.toInt() ?? 1;
        final p = CatalogService.instance.byId(id);
        if (p != null) _items.add(CartItem(product: p, qty: qty));
      }
      notifyListeners();
    } catch (_) {
      await prefs.remove(_key);
    }
  }

  Future<void> add(Product p, {int qty = 1}) async {
    for (final it in _items) {
      if (it.product.id == p.id) {
        it.qty += qty;
        await _persist();
        notifyListeners();
        return;
      }
    }
    _items.add(CartItem(product: p, qty: qty));
    await _persist();
    notifyListeners();
  }

  void remove(Product p) {
    _items.removeWhere((it) => it.product.id == p.id);
    _persist();
    notifyListeners();
  }

  void setQty(Product p, int qty) {
    for (final it in _items) {
      if (it.product.id == p.id) {
        if (qty <= 0) {
          _items.remove(it);
        } else {
          it.qty = qty;
        }
        _persist();
        notifyListeners();
        return;
      }
    }
  }

  void clear() {
    _items.clear();
    _persist();
    notifyListeners();
  }

  Future<void> _persist() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(
        _key,
        jsonEncode(
            _items.map((i) => {'id': i.product.id, 'qty': i.qty}).toList()));
  }
}