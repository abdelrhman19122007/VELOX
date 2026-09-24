import '../core/api_client.dart';
import '../core/app_config.dart';
import '../models/category.dart';
import '../models/product.dart';
import 'mock_catalog.dart';

/// Catalog provider: tries the live backend, falls back to the embedded
/// catalog (identical to the web) when the backend is unreachable or mock
/// mode is enabled.
class CatalogService {
  CatalogService._();

  static final CatalogService instance = CatalogService._();

  List<Product>? _products;
  List<Category> _categories = _defaultCategories();

  bool get hasLoaded => _products != null;

  Future<List<Product>> get products async => _products ??= await load();

  Future<List<Product>> load() async {
    if (useMockApi) {
      _products = mockCatalog;
      _categories = _defaultCategories();
      return _products!;
    }
    try {
      final raw = await ApiClient.instance.request(AppEndpoints.products);
      final list = raw is List ? raw : (raw?['products'] as List? ?? []);
      if (list.isNotEmpty) {
        _products = list
            .map((e) => Product.fromJson(
                Map<String, dynamic>.from(e as Map<String, dynamic>)))
            .toList()
          ..sort((a, b) => a.id.compareTo(b.id));
        await _loadCategories();
        return _products!;
      }
    } catch (_) {
      // fall through to embedded catalog
    }
    _products = mockCatalog;
    _categories = _defaultCategories();
    return _products!;
  }

  Future<void> _loadCategories() async {
    try {
      final raw = await ApiClient.instance.request(AppEndpoints.categories);
      final list = raw is List ? raw : (raw?['categories'] as List? ?? []);
      if (list.isNotEmpty) {
        _categories = list
            .map((e) => Category.fromJson(Map<String, dynamic>.from(e)))
            .toList();
        return;
      }
    } catch (_) {}
    _categories = _defaultCategories();
  }

  List<Category> get categories => _categories;

  List<Product> byCategory(String catId) {
    final all = _products ?? mockCatalog;
    if (catId == 'all' || catId.isEmpty) return all;
    return all.where((p) => p.category == catId).toList();
  }

  Product? byId(int id) {
    final all = _products ?? mockCatalog;
    for (final p in all) {
      if (p.id == id) return p;
    }
    return null;
  }

  static List<Category> _defaultCategories() {
    final food = mockCatalog.where((p) => p.category == 'food').length;
    final fashion = mockCatalog.where((p) => p.category == 'fashion').length;
    final tech = mockCatalog.where((p) => p.category == 'electronics').length;
    return [
      Category(
          id: 'all',
          icon: '✦',
          descAr: 'كل المختارات — ${mockCatalog.length} منتج',
          descEn: 'All picks — ${mockCatalog.length} products'),
      Category(
          id: 'food',
          icon: '🍽️',
          descAr: '$food وجبة ومشروبات',
          descEn: '$food meals & drinks'),
      Category(
          id: 'electronics',
          icon: '⌁',
          descAr: '$tech أجهزة وتقنيات',
          descEn: '$tech tech products'),
      Category(
          id: 'fashion',
          icon: '◈',
          descAr: '$fashion قطعة وإكسسوار',
          descEn: '$fashion fashion picks'),
    ];
  }
}