import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'api.dart';
import 'models.dart';
import 'push.dart';
import 'strings.dart';

/// One line of the cart. The product is kept as it was when added, so the cart shows without reloading the store;
/// the server prices the order again from its own menu anyway (the app never decides a price).
class CartLine {
  final Product product;
  int qty;
  String note;
  CartLine(this.product, this.qty, [this.note = '']);
  Map<String, dynamic> toJson() => {'product': product.toJson(), 'qty': qty, 'note': note};
  factory CartLine.fromJson(Map<String, dynamic> j) =>
      CartLine(Product.fromJson(Map<String, dynamic>.from(j['product'])), j['qty'], j['note'] ?? '');
}

/// Everything the screens share: session, catalogue, cart, settings and live updates.
class AppState extends ChangeNotifier {
  final api = Api();
  late SharedPreferences _prefs;

  String lang = 'en';
  ThemeMode themeMode = ThemeMode.light;
  User? user;
  bool ready = false;

  // catalogue
  List<Zone> zones = [];
  List<Store> stores = [];
  final Map<String, StoreDetail> _details = {};
  String? catalogError;
  Set<String> favorites = {};

  // cart & checkout
  final Map<String, CartLine> cart = {};
  String? cartStoreName;
  String zone = 'SAMANNOUD';
  String promo = '';
  String phone = '', address = '', landmark = '';
  double? lat, lng;

  /// The customer's bottom tab (Home 0 · Orders 1 · Cart 2 · Account 3); any screen can switch it.
  int tab = 0;
  void goTab(int i) {
    tab = i;
    notifyListeners();
  }

  // customer data
  List<OrderView> myOrders = [];
  List<Address> addresses = [];
  int unread = 0;

  LiveStream? _live;
  final Map<String, Set<void Function(OrderView)>> _orderWatchers = {};

  /// Store / driver / admin screens listen to live events here.
  final List<void Function(String event, dynamic data)> extraListeners = [];

  // ---------- helpers ----------
  String t(String key, [Map<String, Object> vars = const {}]) => tr(lang, key, vars);
  bool get ar => lang == 'ar';
  String money(double v) => '${v % 1 == 0 ? v.toStringAsFixed(0) : v.toStringAsFixed(2)} ${t('egp')}';
  String errorText(Object e) {
    if (e is ApiError) return hasText('err_${e.code}') ? t('err_${e.code}') : (e.message.isEmpty ? e.code : e.message);
    return '$e';
  }

  Zone? zoneOf(String code) => zones.where((z) => z.code == code).firstOrNull;
  String zoneName(String code) {
    final z = zoneOf(code);
    return z == null ? code : (ar ? z.nameAr : z.nameEn);
  }

  String storeName(Store s) => ar ? s.nameAr : s.name;
  String productName(Product p) => ar ? p.nameAr : p.name;

  /// The description in the app's language, falling back to the other one.
  String? productDesc(Product p) {
    final first = ar ? p.description : p.descriptionEn;
    final d = (first ?? '').trim().isNotEmpty ? first : (ar ? p.descriptionEn : p.description);
    return (d ?? '').trim().isEmpty ? null : d!.trim();
  }
  String itemName(OrderItem i) => ar ? i.nameAr : i.name;
  bool get isCustomer => user?.role == 'CUSTOMER';

  // ---------- start ----------
  Future<void> init() async {
    _prefs = await SharedPreferences.getInstance();
    serverOverride = _prefs.getString('server');
    lang = _prefs.getString('lang') ?? 'en';
    themeMode = ThemeMode.values[_prefs.getInt('theme') ?? ThemeMode.light.index];
    zone = _prefs.getString('zone') ?? 'SAMANNOUD';
    phone = _prefs.getString('phone') ?? '';
    address = _prefs.getString('address') ?? '';
    landmark = _prefs.getString('landmark') ?? '';
    _loadCart();
    api.onSessionEnded = () {
      if (user != null) signOut();
    };
    final token = _prefs.getString('token');
    if (token != null) {
      api.token = token;
      try {
        user = User.fromJson(await api.get('/auth/me'));
        _afterSignIn();
      } on ApiError catch (e) {
        if (e.code == 'NETWORK') {
          final saved = _prefs.getString('user');
          if (saved != null) user = User.fromJson(jsonDecode(saved));
        } else {
          api.token = null;
          await _prefs.remove('token');
        }
      }
    }
    ready = true;
    notifyListeners();
    loadCatalog();
    checkForUpdate();
  }

  /// This build's number — keep it equal to the +N in pubspec.yaml and to downloads/version.json.
  static const appBuild = 22;

  /// Set when the server offers a newer Android build; the home screen shows an "update" card.
  String? updateUrl;

  /// Android only: a sideloaded APK can't update itself, so it asks the server for the newest build.
  Future<void> checkForUpdate() async {
    if (kIsWeb || defaultTargetPlatform != TargetPlatform.android) return;
    try {
      final res = await http.get(Uri.parse('$apiBase/downloads/version.json')).timeout(const Duration(seconds: 10));
      if (res.statusCode != 200) return;
      final v = jsonDecode(res.body);
      if ((v['build'] as num? ?? 0) > appBuild) {
        updateUrl = '$apiBase/${v['path'] ?? 'downloads/velox.apk'}';
        notifyListeners();
      }
    } catch (_) {}
  }

  bool _discovering = false;

  Future<void> loadCatalog() async {
    try {
      final results = await Future.wait([api.get('/zones'), api.get('/stores')]);
      zones = (results[0] as List).map((z) => Zone.fromJson(z)).where((z) => z.active).toList();
      stores = (results[1] as List).map((s) => Store.fromJson(s)).toList();
      if (zones.isNotEmpty && zoneOf(zone) == null) zone = zones.first.code;
      catalogError = null;
    } catch (e) {
      catalogError = errorText(e);
      // a phone with a dead link: look up the PC's current public link and switch to it by itself
      if (!kIsWeb && e is ApiError && e.code == 'NETWORK' && !_discovering) {
        _discovering = true;
        final found = await discoverServer();
        _discovering = false;
        if (found != null && found != apiBase) {
          await setServer(found);
          return;
        }
      }
    }
    notifyListeners();
  }

  Store? store(String id) => stores.where((s) => s.id == id).firstOrNull;

  Future<StoreDetail> storeDetail(String id, {bool fresh = false}) async {
    if (!fresh && _details.containsKey(id)) return _details[id]!;
    final d = StoreDetail.fromJson(await api.get('/stores/$id'));
    _details[id] = d;
    return d;
  }

  Future<Map<String, dynamic>> search(String q) async => Map<String, dynamic>.from(await api.get('/search?q=${Uri.encodeQueryComponent(q)}'));

  // ---------- settings ----------
  /// Points the app at another server (e.g. a new tunnel link). Empty = back to the built-in one.
  Future<void> setServer(String url) async {
    var u = url.trim();
    while (u.endsWith('/')) {
      u = u.substring(0, u.length - 1);
    }
    serverOverride = u.isEmpty ? null : u;
    u.isEmpty ? await _prefs.remove('server') : await _prefs.setString('server', u);
    stores = [];
    _details.clear();
    catalogError = null;
    notifyListeners();
    await loadCatalog();
    if (user != null) _afterSignIn();
  }

  void toggleLang() {
    lang = ar ? 'en' : 'ar';
    _prefs.setString('lang', lang);
    notifyListeners();
  }

  void setDark(bool dark) {
    themeMode = dark ? ThemeMode.dark : ThemeMode.light;
    _prefs.setInt('theme', themeMode.index);
    notifyListeners();
  }

  // ---------- account ----------
  Future<void> login(String email, String password) async =>
      _session(await api.post('/auth/login', {'email': email.trim(), 'password': password}));

  Future<void> register(String name, String email, String phone, String password, bool driver) async =>
      _session(await api.post('/auth/register', {
        'name': name.trim(),
        'email': email.trim(),
        'phone': phone.trim(),
        'password': password,
        'role': driver ? 'DRIVER' : 'CUSTOMER',
      }));

  void _session(dynamic s) {
    api.token = s['token'];
    user = User.fromJson(Map<String, dynamic>.from(s['user']));
    _prefs.setString('token', api.token!);
    _prefs.setString('user', jsonEncode(user!.toJson()));
    if ((user!.phone ?? '').isNotEmpty && phone.isEmpty) phone = user!.phone!;
    _afterSignIn();
    notifyListeners();
  }

  void _afterSignIn() {
    _live?.close();
    _live = api.live(_onEvent);
    // allowed before? then this device keeps getting notifications without asking again
    Push.enable(api, lang).then((_) => notifyListeners());
    refreshUnread();
    if (isCustomer) {
      refreshOrders();
      loadAddresses();
      api.get('/me/favorites').then((f) {
        favorites = Set<String>.from(f);
        notifyListeners();
      }).catchError((_) {});
    }
  }

  Future<void> changePassword(String current, String password) async =>
      _session(await api.post('/me/password', {'current': current, 'password': password}));

  /// Deletes this account on the server (needs the password), then signs out.
  Future<void> deleteAccount(String password) async {
    await api.post('/me/delete', {'password': password});
    signOut();
  }

  Future<void> logoutEverywhere() async {
    try {
      await api.post('/auth/logout-everywhere');
    } catch (_) {}
    signOut();
  }

  void signOut() {
    _live?.close();
    _live = null;
    Push.disable(api);
    api.token = null;
    user = null;
    myOrders = [];
    addresses = [];
    favorites = {};
    unread = 0;
    tab = 0;
    extraListeners.clear();
    _prefs.remove('token');
    _prefs.remove('user');
    notifyListeners();
  }

  Future<PushState> enablePush() async {
    final s = await Push.enable(api, lang, ask: true);
    notifyListeners();
    return s;
  }

  Future<void> toggleFavorite(String key) async {
    try {
      favorites = Set<String>.from(await api.post('/me/favorites/$key'));
      notifyListeners();
    } catch (_) {}
  }

  // ---------- live updates ----------
  void _onEvent(String event, dynamic data) {
    if (event == 'order' && data is Map) {
      final o = OrderView.fromJson(Map<String, dynamic>.from(data));
      if (isCustomer) {
        final i = myOrders.indexWhere((x) => x.code == o.code);
        if (i >= 0) {
          final changed = myOrders[i].status != o.status;
          myOrders[i] = o;
          if (changed) refreshUnread();
        } else {
          myOrders.insert(0, o);
        }
      } else {
        refreshUnread();
      }
      for (final w in List.of(_orderWatchers[o.code] ?? const <void Function(OrderView)>{})) {
        w(o);
      }
      notifyListeners();
    }
    for (final l in List.of(extraListeners)) {
      l(event, data);
    }
  }

  /// Screens showing an order get its live changes. Several screens may watch the same order.
  void watchOrder(String code, void Function(OrderView) fn) => _orderWatchers.putIfAbsent(code, () => {}).add(fn);
  void unwatchOrder(String code, void Function(OrderView) fn) => _orderWatchers[code]?.remove(fn);

  // ---------- notifications inbox ----------
  Future<List<InboxItem>> inbox() async => (await api.get('/me/notifications') as List).map((n) => InboxItem.fromJson(n)).toList();

  Future<void> refreshUnread() async {
    if (user == null) return;
    try {
      unread = (await inbox()).where((n) => !n.read).length;
      notifyListeners();
    } catch (_) {}
  }

  Future<void> markAllRead() async {
    await api.post('/me/notifications/read');
    unread = 0;
    notifyListeners();
  }

  // ---------- addresses ----------
  Future<void> loadAddresses() async {
    try {
      addresses = (await api.get('/me/addresses') as List).map((a) => Address.fromJson(a)).toList();
      notifyListeners();
    } catch (_) {}
  }

  Future<void> saveAddress(String label) async {
    addresses = (await api.post('/me/addresses', {
      'label': label,
      'zone': zone,
      'address': address,
      'landmark': landmark.isEmpty ? null : landmark,
      'phone': phone,
      'lat': lat,
      'lng': lng,
    }) as List)
        .map((a) => Address.fromJson(a))
        .toList();
    notifyListeners();
  }

  Future<void> deleteAddress(int id) async {
    addresses = (await api.delete('/me/addresses/$id') as List).map((a) => Address.fromJson(a)).toList();
    notifyListeners();
  }

  void useAddress(Address a) {
    zone = a.zone;
    phone = a.phone;
    address = a.address;
    landmark = a.landmark ?? '';
    lat = a.lat;
    lng = a.lng;
    notifyListeners();
  }

  // ---------- orders ----------
  Future<void> refreshOrders() async {
    try {
      myOrders = (await api.get('/orders/mine') as List).map((o) => OrderView.fromJson(o)).toList();
      notifyListeners();
    } catch (_) {}
  }

  Future<OrderView> order(String code) async => OrderView.fromJson(await api.get('/orders/$code'));

  Future<OrderView> cancelOrder(String code) async => _replace(OrderView.fromJson(await api.post('/orders/$code/cancel')));
  Future<OrderView> returnOrder(String code) async => _replace(OrderView.fromJson(await api.post('/orders/$code/return')));

  OrderView _replace(OrderView o) {
    final i = myOrders.indexWhere((x) => x.code == o.code);
    if (i >= 0) myOrders[i] = o;
    notifyListeners();
    return o;
  }

  Future<void> review(String code, int store, int? driver, String comment) =>
      api.post('/orders/$code/review', {'storeRating': store, 'driverRating': driver, if (comment.trim().isNotEmpty) 'comment': comment.trim()});

  Future<String> complain(String code, String details) async =>
      (await api.post('/orders/$code/complaint', {'details': details.trim()}))['code'];

  Future<List<Complaint>> myComplaints() async =>
      (await api.get('/me/complaints') as List).map((c) => Complaint.fromJson(c)).toList();

  /// "Order again" (like the big apps): puts the same items back in the cart when the store still has them.
  /// Returns how many items could not be added.
  Future<int> reorder(OrderView o) async {
    if (o.store == null) return o.items.length;
    final d = await storeDetail(o.store!.id, fresh: true);
    clearCart();
    var missing = 0;
    for (final i in o.items) {
      final p = d.products.where((x) => x.id == i.productId && x.available).firstOrNull;
      if (p == null) {
        missing++;
      } else {
        add(p, d.store, qty: i.qty, note: i.note ?? '');
      }
    }
    return missing;
  }

  // ---------- payments & wallet ----------
  Future<PaymentInfo> payment(String code) async => PaymentInfo.fromJson(await api.get('/orders/$code/payment'));
  Future<void> confirmVodafone(String code, String sms) => api.post('/orders/$code/payment/confirm', {'code': sms});
  Future<void> sandboxPay(String code) => api.post('/orders/$code/payment/sandbox-pay');
  Future<Wallet> wallet() async => Wallet.fromJson(await api.get('/me/wallet'));
  Future<Wallet> topUp(double amount) async => Wallet.fromJson(await api.post('/me/wallet/sandbox-top-up', {'amount': amount}));

  // ---------- cart ----------
  void _loadCart() {
    final saved = _prefs.getString('cart');
    cartStoreName = _prefs.getString('cartStore');
    if (saved == null) return;
    try {
      for (final c in jsonDecode(saved) as List) {
        final line = CartLine.fromJson(Map<String, dynamic>.from(c));
        cart[line.product.id] = line;
      }
    } catch (_) {
      cart.clear();
    }
  }

  void _saveCart() {
    _prefs.setString('cart', jsonEncode(cart.values.map((c) => c.toJson()).toList()));
    cartStoreName == null ? _prefs.remove('cartStore') : _prefs.setString('cartStore', cartStoreName!);
    notifyListeners();
  }

  String? get cartStoreId => cart.isEmpty ? null : cart.values.first.product.storeId;
  int get cartCount => cart.values.fold(0, (n, c) => n + c.qty);
  double get cartSubtotal => cart.values.fold(0.0, (s, c) => s + c.product.price * c.qty);
  int qtyOf(String productId) => cart[productId]?.qty ?? 0;
  bool get cartHasLarge => cart.values.any((c) => c.product.large);

  /// False when the item is from another store (the screen asks, then calls [clearCart] and adds again).
  bool add(Product p, Store store, {int qty = 1, String note = ''}) {
    if (cartStoreId != null && cartStoreId != p.storeId) return false;
    final line = cart.putIfAbsent(p.id, () => CartLine(p, 0));
    line.qty = (line.qty + qty).clamp(1, 99);
    if (note.isNotEmpty) line.note = note;
    cartStoreName = storeName(store);
    _saveCart();
    return true;
  }

  void setQty(String productId, int qty) {
    if (qty <= 0) {
      cart.remove(productId);
    } else {
      cart[productId]?.qty = qty.clamp(1, 99);
    }
    if (cart.isEmpty) cartStoreName = null;
    _saveCart();
  }

  void setNote(String productId, String note) {
    cart[productId]?.note = note;
    _saveCart();
  }

  void clearCart() {
    cart.clear();
    cartStoreName = null;
    _saveCart();
  }

  void setZone(String z) {
    zone = z;
    _prefs.setString('zone', z);
    notifyListeners();
  }

  void setPromo(String code) {
    promo = code.trim().toUpperCase();
    notifyListeners();
  }

  List<Map<String, dynamic>> get _lines => cart.values
      .map((c) => {'productId': c.product.id, 'qty': c.qty, if (c.note.trim().isNotEmpty) 'note': c.note.trim()})
      .toList();

  Future<Quote> quote({bool errand = false}) async => Quote.fromJson(await api.post('/orders/quote', {
        'zone': zone,
        'kind': errand ? 'ERRAND' : 'STORE',
        'items': errand ? [] : _lines,
        if (!errand && promo.isNotEmpty) 'promo': promo,
      }));

  Future<OrderView> placeOrder({
    required String method,
    String? walletPhone,
    double? changeFor,
    DateTime? scheduledFor,
    String? errandDetails,
  }) async {
    final errand = errandDetails != null;
    _prefs.setString('phone', phone);
    _prefs.setString('address', address);
    _prefs.setString('landmark', landmark);
    final o = OrderView.fromJson(await api.post('/orders', {
      'kind': errand ? 'ERRAND' : 'STORE',
      'zone': zone,
      'phone': phone.trim(),
      'address': address.trim(),
      if (landmark.trim().isNotEmpty) 'landmark': landmark.trim(),
      'lat': lat,
      'lng': lng,
      'items': errand ? [] : _lines,
      if (errand) 'errandDetails': errandDetails.trim(),
      'paymentMethod': method,
      if (method == 'VODAFONE_CASH') 'walletPhone': walletPhone?.trim(),
      'cashChangeFor': ?changeFor,
      if (scheduledFor != null) 'scheduledFor': scheduledFor.toUtc().toIso8601String(),
      if (!errand && promo.isNotEmpty) 'promo': promo,
    }));
    if (!errand) {
      clearCart();
      promo = '';
    }
    myOrders.insert(0, o);
    notifyListeners();
    return o;
  }
}
