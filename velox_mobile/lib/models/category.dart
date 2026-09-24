/// Store category — 'all' | 'food' | 'fashion' | 'electronics'.
class Category {
  final String id;
  final String icon; // emoji / symbol
  final String descAr;
  final String descEn;
  final String tint;

  const Category({
    required this.id,
    required this.icon,
    required this.descAr,
    required this.descEn,
    this.tint = '#e9ecff',
  });

  String titleFor(String lang) {
    switch (id) {
      case 'all':
        return lang == 'ar' ? 'كل المختارات' : 'All picks';
      case 'food':
        return lang == 'ar' ? 'أكل و مشروبات' : 'Food & drinks';
      case 'fashion':
        return lang == 'ar' ? 'أزياء و إكسسوارات' : 'Fashion & accessories';
      case 'electronics':
        return lang == 'ar' ? 'إلكترونيات و تقنية' : 'Electronics & tech';
      default:
        return id;
    }
  }

  String descFor(String lang) => lang == 'ar' ? descAr : descEn;

  factory Category.fromJson(Map<String, dynamic> j) => Category(
        id: (j['id'] ?? j['code'] ?? j['category'] ?? 'all').toString(),
        icon: (j['icon'] ?? '✦').toString(),
        descAr: (j['descAr'] ?? j['descriptionAr'] ?? '').toString(),
        descEn: (j['descEn'] ?? j['descriptionEn'] ?? '').toString(),
        tint: (j['tint'] ?? '#e9ecff').toString(),
      );
}