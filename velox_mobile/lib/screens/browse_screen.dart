import 'package:flutter/material.dart';

import '../core/locale.dart';
import '../models/product.dart';
import '../services/catalog_service.dart';
import '../widgets/category_pill.dart';
import '../widgets/empty_state.dart';
import '../widgets/product_card.dart';
import 'product/product_detail_sheet.dart';

class BrowseScreen extends StatefulWidget {
  final String initial;
  final String? searchQuery;

  const BrowseScreen({super.key, this.initial = 'all', this.searchQuery});

  @override
  State<BrowseScreen> createState() => _BrowseScreenState();
}

class _BrowseScreenState extends State<BrowseScreen> {
  late String _cat;
  String _search = '';

  @override
  void initState() {
    super.initState();
    _cat = widget.initial;
    _search = widget.searchQuery ?? '';
  }

  @override
  Widget build(BuildContext context) {
    final categories = CatalogService.instance.categories;
    final products = CatalogService.instance.byCategory(_cat);

    final filtered =
        _search.trim().isEmpty ? products : _searchFilter(products);

    return Scaffold(
      appBar: AppBar(
        title: Text(_search.trim().isEmpty
            ? AppLocale.tr('nav.browse')
            : AppLocale.t('نتائج البحث', 'Search results')),
      ),
      body: Column(
        children: [
          if (_search.trim().isNotEmpty)
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 4),
              child: Align(
                alignment: Alignment.centerLeft,
                child: Chip(
                  label: Text(_search.trim()),
                  deleteIcon: const Icon(Icons.close, size: 16),
                  onDeleted: () => setState(() => _search = ''),
                ),
              ),
            ),
          // Category pills
          SizedBox(
            height: 46,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
              itemCount: categories.length,
              separatorBuilder: (_, __) => const SizedBox(width: 8),
              itemBuilder: (_, i) {
                final c = categories[i];
                return CategoryPill(
                  id: c.id,
                  title: c.titleFor(AppLocale.lang),
                  active: _cat == c.id,
                  onTap: () => setState(() {
                    _cat = c.id;
                    _search = '';
                  }),
                );
              },
            ),
          ),
          const SizedBox(height: 4),
          Expanded(
            child: filtered.isEmpty
                ? EmptyState(
                    icon: Icons.search_off,
                    title: AppLocale.t('لا توجد منتجات', 'No products found'),
                    hint: AppLocale.t(
                        'جرّب فئة أخرى أو ابحث بكلمات مختلفة',
                        'Try another category or different keywords'),
                  )
                : GridView.builder(
                    padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
                    gridDelegate:
                        const SliverGridDelegateWithFixedCrossAxisCount(
                      crossAxisCount: 2,
                      mainAxisSpacing: 12,
                      crossAxisSpacing: 12,
                      childAspectRatio: 0.72,
                    ),
                    itemCount: filtered.length,
                    itemBuilder: (_, i) => ProductCard(
                      product: filtered[i],
                      onTap: () => showProductDetail(context, filtered[i]),
                    ),
                  ),
          ),
        ],
      ),
    );
  }

  List<Product> _searchFilter(List<Product> list) {
    final q = _search.trim().toLowerCase();
    return list.where((p) {
      final hay = '${p.nameAr} ${p.nameEn} ${p.storeAr} ${p.storeEn}'
          .toLowerCase();
      return hay.contains(q);
    }).toList();
  }
}