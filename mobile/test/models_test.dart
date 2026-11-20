import 'package:flutter_test/flutter_test.dart';
import 'package:velox_app/core/models.dart';
import 'package:velox_app/core/strings.dart';

/// The app reads exactly what the server sends (same field names as the API).
void main() {
  group('OrderView', () {
    final json = {
      'code': 'ORD-1001',
      'kind': 'STORE',
      'status': 'PICKED_UP',
      'store': {'id': 'R1', 'name': 'Koshary', 'nameAr': 'كشري', 'lat': 30.96, 'lng': 31.24},
      'zone': 'SAMANNOUD',
      'phone': '01012345678',
      'address': 'شارع البحر',
      'landmark': 'جنب الجامع',
      'items': [
        {'productId': 'F1', 'name': 'Koshary', 'nameAr': 'كشري', 'unitPrice': 45, 'qty': 2, 'note': 'من غير بصل'},
      ],
      'subtotal': 90,
      'discount': 9,
      'deliveryFee': 15,
      'total': 96,
      'paymentMethod': 'FAWRY',
      'paymentStatus': 'PAID',
      'driver': {'name': 'مندوب', 'phone': '01112345678', 'lat': 30.961, 'lng': 31.240},
      'timeline': [
        {'status': 'PLACED', 'at': '2026-10-26T00:39:00Z'},
        {'status': 'PICKED_UP', 'at': '2026-10-26T00:50:00Z'},
      ],
      'createdAt': '2026-10-26T00:39:00Z',
    };

    test('parses money, items, driver and timeline', () {
      final o = OrderView.fromJson(json);
      expect(o.total, 96);
      expect(o.items.single.lineTotal, 90);
      expect(o.items.single.note, 'من غير بصل');
      expect(o.driver!.name, 'مندوب');
      expect(o.store!.nameAr, 'كشري');
      expect(o.stepAt('PICKED_UP'), isNotNull);
      expect(o.stepAt('DELIVERED'), isNull);
    });

    test('knows active, finished and errand orders', () {
      final o = OrderView.fromJson(json);
      expect(o.active, isTrue);
      expect(o.finished, isFalse);
      expect(o.isErrand, isFalse);
      final done = OrderView.fromJson({...json, 'status': 'DELIVERED', 'kind': 'ERRAND', 'store': null});
      expect(done.finished, isTrue);
      expect(done.isErrand, isTrue);
      expect(done.store, isNull);
    });
  });

  test('Product keeps the size for tuk-tuk items', () {
    final p = Product.fromJson({
      'id': 'F9', 'storeId': 'G1', 'name': 'Gas', 'nameAr': 'أنبوبة', 'price': 180, 'size': 'LARGE', 'section': 'GAS', 'available': true,
    });
    expect(p.large, isTrue);
    expect(Product.fromJson(p.toJson()).price, 180);
  });

  test('Store is orderable only when open and accepting', () {
    Store s(bool open, bool accepting) => Store.fromJson({
          'id': 'R1', 'type': 'FOOD', 'cuisine': 'KOSHARY', 'name': 'K', 'nameAr': 'ك', 'accepting': accepting, 'openNow': open, 'ratingCount': 0,
        });
    expect(s(true, true).orderable, isTrue);
    expect(s(false, true).orderable, isFalse);
    expect(s(true, false).orderable, isFalse);
  });

  group('texts', () {
    test('Arabic and English for the same key', () {
      expect(tr('ar', 'st_DELIVERED'), 'وصل');
      expect(tr('en', 'st_DELIVERED'), 'Delivered');
    });

    test('every server error the app may show has a translation', () {
      for (final code in ['BAD_LOGIN', 'WRONG_CODE', 'PAYMENT_EXPIRED', 'ALREADY_TAKEN', 'INVALID_PROMO', 'STORE_CLOSED']) {
        expect(hasText('err_$code'), isTrue, reason: code);
      }
    });
  });
}
