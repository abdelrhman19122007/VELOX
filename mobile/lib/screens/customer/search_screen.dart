import 'dart:async';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/app_state.dart';
import '../../core/models.dart';
import '../../widgets/common.dart';
import 'home_screen.dart';
import 'store_screen.dart';

/// Search stores and dishes (server-side search, as the user types).
class SearchScreen extends StatefulWidget {
  const SearchScreen({super.key});
  @override
  State<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends State<SearchScreen> {
  Timer? _debounce;
  List<Store> stores = [];
  List<Product> products = [];
  bool loading = false;
  String? error;
  String q = '';

  void _changed(String v) {
    q = v;
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 350), _run);
  }

  Future<void> _run() async {
    final s = context.read<AppState>();
    if (q.trim().length < 2) {
      setState(() {
        stores = [];
        products = [];
      });
      return;
    }
    setState(() => loading = true);
    try {
      final r = await s.search(q.trim());
      stores = (r['stores'] as List).map((x) => Store.fromJson(x)).toList();
      products = (r['products'] as List).map((x) => Product.fromJson(x)).toList();
      error = null;
    } catch (e) {
      error = s.errorText(e);
    }
    if (mounted) setState(() => loading = false);
  }

  @override
  void dispose() {
    _debounce?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(
        title: TextField(
          autofocus: true,
          onChanged: _changed,
          decoration: InputDecoration(hintText: s.t('searchPh'), border: InputBorder.none, filled: false),
        ),
      ),
      body: loading
          ? const LinearProgressIndicator()
          : error != null
              ? EmptyState(Icons.cloud_off, error!)
              : (stores.isEmpty && products.isEmpty)
                  ? EmptyState(Icons.search, q.trim().length < 2 ? s.t('searchPh') : s.t('noStores'))
                  : ListView(padding: const EdgeInsets.only(bottom: 24), children: [
                      if (stores.isNotEmpty) SectionTitle(s.t('stores')),
                      for (final st in stores) Padding(padding: const EdgeInsets.fromLTRB(16, 0, 16, 12), child: StoreCard(st)),
                      if (products.isNotEmpty) SectionTitle(s.t('menu')),
                      for (final p in products)
                        ListTile(
                          leading: ProductImage(p.photoUrl, size: 52),
                          title: Text(s.productName(p), style: const TextStyle(fontWeight: FontWeight.w700)),
                          subtitle: Text(s.store(p.storeId) == null ? '' : s.storeName(s.store(p.storeId)!)),
                          trailing: Text(s.money(p.price), style: TextStyle(color: cs.primary, fontWeight: FontWeight.w800)),
                          onTap: () =>
                              Navigator.push(context, MaterialPageRoute(builder: (_) => StoreScreen(storeId: p.storeId))),
                        ),
                    ]),
    );
  }
}
