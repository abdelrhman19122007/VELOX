/// Product entity — mirrors the web catalog (id, bilingual names, category,
/// price, store, image) and tolerates both API and mock shapes.
class Product {
  final int id;
  final String nameAr;
  final String nameEn;
  final String storeAr;
  final String storeEn;
  final String category; // food | fashion | electronics
  final double price;
  final String image;
  final String descAr;
  final String descEn;
  final String badge;

  const Product({
    required this.id,
    required this.nameAr,
    required this.nameEn,
    required this.storeAr,
    required this.storeEn,
    required this.category,
    required this.price,
    required this.image,
    this.descAr = '',
    this.descEn = '',
    this.badge = '',
  });

  bool get isFood => category == 'food';

  String nameFor(String lang) => lang == 'ar' ? nameAr : nameEn;

  String storeFor(String lang) => lang == 'ar' ? storeAr : storeEn;

  String descFor(String lang) => lang == 'ar' ? descAr : descEn;

  /// Resolve an asset path or remote URL into something Image can load.
  String imageSource() =>
      image.startsWith('http') || image.startsWith('assets/')
          ? image
          : 'assets/images/products/$image';

  factory Product.fromJson(Map<String, dynamic> j) {
    return Product(
      id: (j['id'] as num?)?.toInt() ?? 0,
      nameAr: (j['nameAr'] ?? j['name_ar'] ?? j['name'] ?? '').toString(),
      nameEn: (j['nameEn'] ?? j['name_en'] ?? j['name'] ?? '').toString(),
      storeAr:
          (j['storeAr'] ?? j['store_ar'] ?? j['storeNameAr'] ?? '').toString(),
      storeEn: (j['storeEn'] ?? j['store_en'] ?? j['storeNameEn'] ?? '')
          .toString(),
      category: (j['category'] ?? j['type'] ?? 'food').toString(),
      price: (j['price'] as num?)?.toDouble() ?? 0,
      image: (j['image'] ?? j['imageUrl'] ?? '').toString(),
      descAr: (j['descAr'] ?? j['descriptionAr'] ?? '').toString(),
      descEn: (j['descEn'] ?? j['descriptionEn'] ?? '').toString(),
      badge: (j['badge'] ?? '').toString(),
    );
  }
}