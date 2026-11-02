// The data the app gets from the VELOX API — same field names as the server's DTOs (see /swagger-ui.html).

double _d(dynamic v) => v == null ? 0 : (v as num).toDouble();
double? _dn(dynamic v) => v == null ? null : (v as num).toDouble();
DateTime? _t(dynamic v) => v == null ? null : DateTime.tryParse('$v')?.toLocal();

class User {
  final String id, email, name, role;
  final String? phone, storeId;
  User({required this.id, required this.email, required this.name, required this.role, this.phone, this.storeId});
  factory User.fromJson(Map<String, dynamic> j) =>
      User(id: j['id'], email: j['email'], name: j['name'], role: j['role'], phone: j['phone'], storeId: j['storeId']);
  Map<String, dynamic> toJson() => {'id': id, 'email': email, 'name': name, 'role': role, 'phone': phone, 'storeId': storeId};
}

class Zone {
  final String code, nameAr, nameEn, kind;
  final double fee;
  final int etaMinutes;
  final bool active;
  Zone(this.code, this.nameAr, this.nameEn, this.kind, this.fee, this.etaMinutes, this.active);
  factory Zone.fromJson(Map<String, dynamic> j) => Zone(j['code'], j['nameAr'], j['nameEn'], j['kind'] ?? 'VILLAGE',
      _d(j['fee']), (j['etaMinutes'] as num?)?.toInt() ?? 30, j['active'] ?? true);
}

class Store {
  final String id, type, cuisine, name, nameAr;
  final String? tagline, taglineAr, photoUrl, opens, closes;
  final bool accepting, openNow;
  final double? rating, lat, lng;
  final int ratingCount;
  Store({
    required this.id,
    required this.type,
    required this.cuisine,
    required this.name,
    required this.nameAr,
    this.tagline,
    this.taglineAr,
    this.photoUrl,
    this.opens,
    this.closes,
    required this.accepting,
    required this.openNow,
    this.rating,
    required this.ratingCount,
    this.lat,
    this.lng,
  });
  factory Store.fromJson(Map<String, dynamic> j) => Store(
        id: j['id'],
        type: j['type'],
        cuisine: j['cuisine'] ?? '',
        name: j['name'],
        nameAr: j['nameAr'],
        tagline: j['tagline'],
        taglineAr: j['taglineAr'],
        photoUrl: j['photoUrl'],
        opens: j['opens'],
        closes: j['closes'],
        accepting: j['accepting'] ?? true,
        openNow: j['openNow'] ?? true,
        rating: _dn(j['rating']),
        ratingCount: (j['ratingCount'] as num?)?.toInt() ?? 0,
        lat: _dn(j['lat']),
        lng: _dn(j['lng']),
      );
  bool get orderable => openNow && accepting;
}

class Product {
  final String id, storeId, name, nameAr, size, section;
  final String? description, descriptionEn, photoUrl;
  final double price;
  final bool available;
  Product({
    required this.id,
    required this.storeId,
    required this.name,
    required this.nameAr,
    required this.size,
    required this.section,
    this.description,
    this.descriptionEn,
    this.photoUrl,
    required this.price,
    required this.available,
  });
  factory Product.fromJson(Map<String, dynamic> j) => Product(
        id: j['id'],
        storeId: j['storeId'],
        name: j['name'],
        nameAr: j['nameAr'],
        size: j['size'] ?? 'MEDIUM',
        section: j['section'] ?? 'OTHER',
        description: j['description'],
        descriptionEn: j['descriptionEn'],
        photoUrl: j['photoUrl'],
        price: _d(j['price']),
        available: j['available'] ?? true,
      );
  Map<String, dynamic> toJson() => {
        'id': id,
        'storeId': storeId,
        'name': name,
        'nameAr': nameAr,
        'size': size,
        'section': section,
        'description': description,
        'descriptionEn': descriptionEn,
        'photoUrl': photoUrl,
        'price': price,
        'available': available,
      };
  bool get large => size == 'LARGE';
}

class Section {
  final String code;
  final List<Product> products;
  Section(this.code, this.products);
}

class StoreDetail {
  final Store store;
  final String? phone, address;
  final List<Section> sections;
  StoreDetail(this.store, this.phone, this.address, this.sections);
  factory StoreDetail.fromJson(Map<String, dynamic> j) => StoreDetail(
        Store.fromJson(j['store']),
        j['phone'],
        j['address'],
        (j['sections'] as List? ?? [])
            .map((s) => Section(s['code'], (s['products'] as List).map((p) => Product.fromJson(p)).toList()))
            .toList(),
      );
  List<Product> get products => [for (final s in sections) ...s.products];
}

class Quote {
  final double subtotal, deliveryFee, discount, total;
  final int etaMinutes;
  Quote(this.subtotal, this.deliveryFee, this.discount, this.total, this.etaMinutes);
  factory Quote.fromJson(Map<String, dynamic> j) => Quote(_d(j['subtotal']), _d(j['deliveryFee']), _d(j['discount']),
      _d(j['total']), (j['etaMinutes'] as num?)?.toInt() ?? 0);
}

class OrderItem {
  final String productId, name, nameAr;
  final double unitPrice;
  final int qty;
  final String? note;
  OrderItem(this.productId, this.name, this.nameAr, this.unitPrice, this.qty, this.note);
  factory OrderItem.fromJson(Map<String, dynamic> j) =>
      OrderItem(j['productId'] ?? '', j['name'] ?? '', j['nameAr'] ?? '', _d(j['unitPrice']), (j['qty'] as num).toInt(), j['note']);
  double get lineTotal => unitPrice * qty;
}

class OrderStep {
  final String status;
  final DateTime? at;
  OrderStep(this.status, this.at);
}

class StoreRef {
  final String id, name, nameAr;
  final double? lat, lng;
  StoreRef(this.id, this.name, this.nameAr, this.lat, this.lng);
}

class DriverRef {
  final String name;
  final String? phone;
  final double? lat, lng;
  DriverRef(this.name, this.phone, this.lat, this.lng);
}

class OrderView {
  final String code, kind, status, zone, phone, address, paymentMethod, paymentStatus;
  final String? landmark, errandDetails, rejectReason, customerName;
  final double? lat, lng, cashChangeFor;
  final double subtotal, discount, deliveryFee, total;
  final StoreRef? store;
  final DriverRef? driver;
  final List<OrderItem> items;
  final List<OrderStep> timeline;
  final DateTime? scheduledFor, createdAt;

  OrderView({
    required this.code,
    required this.kind,
    required this.status,
    required this.zone,
    required this.phone,
    required this.address,
    required this.paymentMethod,
    required this.paymentStatus,
    this.landmark,
    this.errandDetails,
    this.rejectReason,
    this.customerName,
    this.lat,
    this.lng,
    this.cashChangeFor,
    required this.subtotal,
    required this.discount,
    required this.deliveryFee,
    required this.total,
    this.store,
    this.driver,
    required this.items,
    required this.timeline,
    this.scheduledFor,
    this.createdAt,
  });

  factory OrderView.fromJson(Map<String, dynamic> j) {
    final s = j['store'], d = j['driver'];
    return OrderView(
      code: j['code'],
      kind: j['kind'] ?? 'STORE',
      status: j['status'],
      zone: j['zone'] ?? '',
      phone: j['phone'] ?? '',
      address: j['address'] ?? '',
      paymentMethod: j['paymentMethod'] ?? 'CASH',
      paymentStatus: j['paymentStatus'] ?? 'COD',
      landmark: j['landmark'],
      errandDetails: j['errandDetails'],
      rejectReason: j['rejectReason'],
      customerName: j['customerName'],
      lat: _dn(j['lat']),
      lng: _dn(j['lng']),
      cashChangeFor: _dn(j['cashChangeFor']),
      subtotal: _d(j['subtotal']),
      discount: _d(j['discount']),
      deliveryFee: _d(j['deliveryFee']),
      total: _d(j['total']),
      store: s == null ? null : StoreRef(s['id'], s['name'], s['nameAr'], _dn(s['lat']), _dn(s['lng'])),
      driver: d == null ? null : DriverRef(d['name'] ?? '', d['phone'], _dn(d['lat']), _dn(d['lng'])),
      items: (j['items'] as List? ?? []).map((i) => OrderItem.fromJson(i)).toList(),
      timeline: (j['timeline'] as List? ?? []).map((t) => OrderStep(t['status'], _t(t['at']))).toList(),
      scheduledFor: _t(j['scheduledFor']),
      createdAt: _t(j['createdAt']),
    );
  }

  bool get isErrand => kind == 'ERRAND';
  bool get active => const {'PLACED', 'ACCEPTED', 'READY', 'PICKED_UP'}.contains(status);
  bool get waitingPayment => status == 'PENDING_PAYMENT';
  bool get finished => const {'DELIVERED', 'REJECTED', 'CANCELLED', 'RETURNED'}.contains(status);
  DateTime? stepAt(String s) => timeline.where((t) => t.status == s).firstOrNull?.at;
}

class PaymentInfo {
  final String orderCode, method, status;
  final double amount;
  final String? reference, walletPhone, sandboxCode;
  final DateTime? expiresAt;
  final bool sandbox;
  PaymentInfo(this.orderCode, this.method, this.status, this.amount, this.reference, this.walletPhone, this.expiresAt,
      this.sandbox, this.sandboxCode);
  factory PaymentInfo.fromJson(Map<String, dynamic> j) => PaymentInfo(j['orderCode'], j['method'], j['status'],
      _d(j['amount']), j['reference'], j['walletPhone'], _t(j['expiresAt']), j['sandbox'] ?? false, j['sandboxCode']);
}

class WalletTx {
  final double amount;
  final String reason;
  final DateTime? at;
  WalletTx(this.amount, this.reason, this.at);
}

class Wallet {
  final double balance;
  final int points;
  final List<WalletTx> history;
  Wallet(this.balance, this.points, this.history);
  factory Wallet.fromJson(Map<String, dynamic> j) => Wallet(_d(j['balance']), (j['points'] as num?)?.toInt() ?? 0,
      (j['history'] as List? ?? []).map((t) => WalletTx(_d(t['amount']), t['reason'] ?? '', _t(t['at']))).toList());
}

class InboxItem {
  final int id;
  final String title, body;
  final String? orderCode;
  final bool read;
  final DateTime? at;
  InboxItem(this.id, this.title, this.body, this.orderCode, this.read, this.at);
  factory InboxItem.fromJson(Map<String, dynamic> j) =>
      InboxItem((j['id'] as num).toInt(), j['title'] ?? '', j['body'] ?? '', j['orderCode'], j['read'] ?? false, _t(j['at']));
}

class Address {
  final int id;
  final String label, zone, address, phone;
  final String? landmark;
  final double? lat, lng;
  Address(this.id, this.label, this.zone, this.address, this.landmark, this.phone, this.lat, this.lng);
  factory Address.fromJson(Map<String, dynamic> j) => Address((j['id'] as num).toInt(), j['label'] ?? '', j['zone'] ?? '',
      j['address'] ?? '', j['landmark'], j['phone'] ?? '', _dn(j['lat']), _dn(j['lng']));
}

class Complaint {
  final String code, orderCode, details, status;
  final String? compensationCode, adminNote;
  final DateTime? at;
  Complaint(this.code, this.orderCode, this.details, this.status, this.compensationCode, this.adminNote, this.at);
  factory Complaint.fromJson(Map<String, dynamic> j) => Complaint(j['code'], j['orderCode'] ?? '', j['details'] ?? '',
      j['status'] ?? 'OPEN', j['compensationCode'], j['adminNote'], _t(j['at']));
}

class Review {
  final String orderCode;
  final int storeRating;
  final int? driverRating;
  final String? comment;
  final DateTime? at;
  Review(this.orderCode, this.storeRating, this.driverRating, this.comment, this.at);
  factory Review.fromJson(Map<String, dynamic> j) => Review(j['orderCode'] ?? '', (j['storeRating'] as num).toInt(),
      (j['driverRating'] as num?)?.toInt(), j['comment'], _t(j['at']));
}
