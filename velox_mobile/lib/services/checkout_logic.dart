import '../core/api_client.dart';
import '../core/app_config.dart';
import '../core/locale.dart';

/// Governorate metadata + delivery math — ported 1:1 from the web's
/// checkoutPage.js (GOV_SHIPPING / GOV_DAYS / GOV_HOURS / mapCityToGov).
class GovernorateInfo {
  final String id;
  final String nameAr;
  final int shippingFee;
  final int etaDays;
  final int foodEtaMinutes;

  const GovernorateInfo(
    this.id,
    this.nameAr,
    this.shippingFee,
    this.etaDays,
    this.foodEtaMinutes,
  );

  String get nameEn => id.replaceAll('_', ' ').toLowerCase();

  String nameFor(String lang) => lang == 'ar' ? nameAr : nameEn;
}

class CheckoutData {
  CheckoutData._();

  static const List<GovernorateInfo> all = [
    GovernorateInfo('CAIRO', 'القاهرة', 20, 1, 45),
    GovernorateInfo('GIZA', 'الجيزة', 25, 1, 50),
    GovernorateInfo('ALEXANDRIA', 'الإسكندرية', 40, 2, 90),
    GovernorateInfo('DAMIETTA', 'دمياط', 60, 3, 100),
    GovernorateInfo('BEHEIRA', 'البحيرة', 50, 2, 80),
    GovernorateInfo('KAFR_EL_SHEIKH', 'كفر الشيخ', 55, 3, 90),
    GovernorateInfo('GHARBIA', 'الغربية', 50, 2, 75),
    GovernorateInfo('MENOFIA', 'المنوفية', 45, 2, 70),
    GovernorateInfo('QALYUBIA', 'القليوبية', 30, 1, 55),
    GovernorateInfo('SHARKIA', 'الشرقية', 45, 2, 80),
    GovernorateInfo('DAKAHLIA', 'الدقهلية', 50, 2, 80),
    GovernorateInfo('PORT_SAID', 'بورسعيد', 60, 3, 100),
    GovernorateInfo('ISMAILIA', 'الإسماعيلية', 55, 3, 95),
    GovernorateInfo('SUEZ', 'السويس', 55, 3, 95),
    GovernorateInfo('NORTH_SINAI', 'شمال سيناء', 75, 5, 140),
    GovernorateInfo('SOUTH_SINAI', 'جنوب سيناء', 75, 4, 130),
    GovernorateInfo('FAYOUM', 'الفيوم', 45, 2, 75),
    GovernorateInfo('BENI_SUEF', 'بني سويف', 50, 2, 85),
    GovernorateInfo('MINYA', 'المنيا', 60, 3, 100),
    GovernorateInfo('ASYUT', 'أسيوط', 65, 3, 110),
    GovernorateInfo('SOHAG', 'سوهاج', 65, 3, 115),
    GovernorateInfo('QENA', 'قنا', 70, 4, 120),
    GovernorateInfo('LUXOR', 'الأقصر', 70, 4, 120),
    GovernorateInfo('ASWAN', 'أسوان', 75, 4, 130),
    GovernorateInfo('NEW_VALLEY', 'الوادي الجديد', 80, 5, 150),
    GovernorateInfo('MATROUH', 'مطروح', 70, 4, 120),
    GovernorateInfo('RED_SEA', 'البحر الأحمر', 70, 4, 110),
  ];

  static GovernorateInfo byId(String id) => all.firstWhere(
        (g) => g.id == id,
        orElse: () => all.first,
      );

  /// Reverse-geocode city/region keywords -> governorate id.
  static const List<List<String>> cityToGovKeys = [
    ['القاهرة|cairo', 'CAIRO'],
    ['الجيزة|giza', 'GIZA'],
    ['الإسكندرية|الاسكندرية|alexandria', 'ALEXANDRIA'],
    ['دمياط|damietta', 'DAMIETTA'],
    ['البحيرة|beheira|دمنهور|damanhur', 'BEHEIRA'],
    ['كفر الشيخ|kafr', 'KAFR_EL_SHEIKH'],
    ['الغربية|gharbia|طنطا|tanta', 'GHARBIA'],
    ['المنوفية|menofia|menoufia|شبين|shebin', 'MENOFIA'],
    ['القليوبية|qalyubia|بنها|banha|qalubia', 'QALYUBIA'],
    ['الشرقية|sharkia|sharqia|الزقازيق|zagazig', 'SHARKIA'],
    ['الدقهلية|dakahlia|المنصورة|mansoura', 'DAKAHLIA'],
    ['بورسعيد|port said|portsaid', 'PORT_SAID'],
    ['الإسماعيلية|الاسماعيلية|ismailia', 'ISMAILIA'],
    ['السويس|suez', 'SUEZ'],
    ['شمال سيناء|north sinai|العريش|arish|arich', 'NORTH_SINAI'],
    ['جنوب سيناء|south sinai|شرم الشيخ|sharm', 'SOUTH_SINAI'],
    ['الفيوم|fayoum|faiyum', 'FAYOUM'],
    ['بني سويف|beni suef|banisuif', 'BENI_SUEF'],
    ['المنيا|minya|minia', 'MINYA'],
    ['أسيوط|اسيوط|asyut|assiut', 'ASYUT'],
    ['سوهاج|sohag|suhag', 'SOHAG'],
    ['قنا|qena|qina', 'QENA'],
    ['الأقصر|الاقصر|luxor|luqsor', 'LUXOR'],
    ['أسوان|اسوان|aswan', 'ASWAN'],
    ['الوادي الجديد|new valley|الخارجة|kharga', 'NEW_VALLEY'],
    ['مطروح|matrouh|matruh|مرسى مطروح|marsa', 'MATROUH'],
    ['البحر الأحمر|red sea|الغردقة|hurghada', 'RED_SEA'],
  ];

  static String? mapCityToGov(String text) {
    final hay = ' ${text.toLowerCase()} ';
    for (final entry in cityToGovKeys) {
      for (final k in entry.first.split('|')) {
        if (k.isNotEmpty && hay.contains(k.toLowerCase())) return entry.last;
      }
    }
    return null;
  }

  /// Food -> minutes; fashion/electronics -> days (based on location).
  static String etaText(String govId, {required bool foodOnly}) {
    final g = byId(govId);
    if (foodOnly) {
      final m = g.foodEtaMinutes;
      if (AppLocale.isAr) {
        return m <= 60
            ? 'يصل الطلب خلال $m دقيقة'
            : 'يصل الطلب خلال ${(m / 60).toStringAsFixed(1)} ساعة';
      }
      return m <= 60
          ? 'Arrives within $m minutes'
          : 'Arrives within ${(m / 60).toStringAsFixed(1)} hours';
    }
    final d = g.etaDays;
    return AppLocale.isAr
        ? 'يصل الطلب خلال $d ${d == 1 ? 'يوم' : 'أيام'}'
        : 'Arrives within $d day${d > 1 ? 's' : ''}';
  }

  static int shippingFee(String govId, {double extraStoresFee = 0}) =>
      byId(govId).shippingFee + extraStoresFee.round();

  /// Build the shipping address string stored on the order:
  /// governorate + street address (falls back to phone when no street given).
  static String buildShippingAddress({
    required String govId,
    required String address,
    required String phone,
  }) {
    final detail = address.trim();
    return detail.isEmpty
        ? '${byId(govId).nameFor('en')}, $phone'
        : '${byId(govId).nameFor('en')} - $detail';
  }
}

/// Combines shipping + a consolidation fee per extra store, mirroring the
/// backend's MultiStorePricing.extraFee.
class DeliveryQuote {
  final double subtotal;
  final double shipping;
  final double discount;
  final double total;
  final String governorate;
  final String? promoCode;

  const DeliveryQuote({
    required this.subtotal,
    required this.shipping,
    required this.discount,
    required this.total,
    required this.governorate,
    this.promoCode,
  });

  factory DeliveryQuote.fromApi(
    Map<String, dynamic> j, {
    double fallbackSubtotal = 0,
    double fallbackShipping = 20,
  }) {
    final total = (j['total'] ?? j['total_amount'] ?? 0) as num;
    final subtotal = (j['subtotal'] ?? fallbackSubtotal) as num;
    final shipping = (j['delivery_fee'] ?? j['shipping'] ?? fallbackShipping)
        as num;
    final discount = (j['discount'] ?? 0) as num;
    return DeliveryQuote(
      subtotal: subtotal.toDouble(),
      shipping: shipping.toDouble(),
      discount: discount.toDouble(),
      total: total.toDouble(),
      governorate: (j['governorate'] ?? 'CAIRO').toString(),
      promoCode: (j['promoCode'] ?? j['promo_code'] ?? j['coupon'])
          ?.toString(),
    );
  }

  factory DeliveryQuote.local({
    required double subtotal,
    required double shipping,
    double discount = 0,
    required String governorate,
    String? promoCode,
  }) =>
      DeliveryQuote(
        subtotal: subtotal,
        shipping: shipping,
        discount: discount,
        total: (subtotal + shipping - discount).clamp(0, double.infinity),
        governorate: governorate,
        promoCode: promoCode,
      );
}

/// Server quote helper (fires POST /api/orders/quote when backend is up).
Future<DeliveryQuote?> fetchQuote({
  required List<Map<String, dynamic>> items,
  required String governorate,
  required double subtotal,
}) async {
  if (useMockApi) return null;
  try {
    final data = await ApiClient.instance.request(
      AppEndpoints.orderQuote,
      method: 'POST',
      body: {'items': items, 'governorate': governorate},
    );
    if (data is Map) {
      return DeliveryQuote.fromApi(Map<String, dynamic>.from(data));
    }
  } catch (_) {}
  return null;
}