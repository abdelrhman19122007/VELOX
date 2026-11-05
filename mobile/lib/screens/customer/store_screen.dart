import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/api.dart';
import '../../core/app_state.dart';
import '../../core/models.dart';
import '../../widgets/common.dart';

/// A store's page: header, hours, menu by section with search, add-with-note, and a sticky cart bar.
class StoreScreen extends StatefulWidget {
  final String storeId;
  const StoreScreen({super.key, required this.storeId});
  @override
  State<StoreScreen> createState() => _StoreScreenState();
}

class _StoreScreenState extends State<StoreScreen> {
  late Future<StoreDetail> detail;
  String query = '';
  String? section;

  late Future<List<dynamic>> reviews;

  @override
  void initState() {
    super.initState();
    detail = context.read<AppState>().storeDetail(widget.storeId, fresh: true);
    reviews = context.read<AppState>().api.get('/stores/${widget.storeId}/reviews').then((r) => r as List).catchError((_) => <dynamic>[]);
  }

  /// Opens the dish page; whatever it returns (quantity, note) goes in the cart — one store per cart.
  Future<void> _add(AppState s, Store store, Product p) async {
    final picked = await Navigator.push<(int, String)>(
        context, MaterialPageRoute(builder: (_) => _DishPage(product: p, store: store, note: s.cart[p.id]?.note ?? '')));
    if (picked == null || !mounted) return;
    final (qty, note) = picked;
    if (!s.add(p, store, qty: qty, note: note)) {
      final fresh = await showDialog<bool>(
        context: context,
        builder: (c) => AlertDialog(
          title: Text(s.t('otherStoreTitle')),
          content: Text(s.t('otherStoreBody')),
          actions: [
            TextButton(onPressed: () => Navigator.pop(c, false), child: Text(s.t('cancel'))),
            FilledButton(onPressed: () => Navigator.pop(c, true), child: Text(s.t('startNew'))),
          ],
        ),
      );
      if (fresh == true) {
        s.clearCart();
        s.add(p, store, qty: qty, note: note);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    return FutureBuilder<StoreDetail>(
      future: detail,
      builder: (c, snap) {
        if (snap.hasError) {
          return Scaffold(
            appBar: AppBar(),
            body: EmptyState(Icons.cloud_off, s.errorText(snap.error!),
                action: FilledButton(
                    onPressed: () => setState(() => detail = s.storeDetail(widget.storeId, fresh: true)), child: Text(s.t('retry')))),
          );
        }
        if (!snap.hasData) return const Scaffold(body: Center(child: CircularProgressIndicator()));
        final d = snap.data!;
        final store = d.store;
        final q = query.trim().toLowerCase();
        final sections = d.sections
            .where((sec) => section == null || sec.code == section)
            .map((sec) => Section(
                sec.code,
                sec.products
                    .where((p) => q.isEmpty || p.name.toLowerCase().contains(q) || p.nameAr.contains(q))
                    .toList()))
            .where((sec) => sec.products.isNotEmpty)
            .toList();
        final inCart = s.cartStoreId == store.id ? s.cartCount : 0;
        final fav = s.favorites.contains('store:${store.id}');
        return Scaffold(
          body: CustomScrollView(slivers: [
            SliverAppBar(
              pinned: true,
              expandedHeight: 190,
              actions: [
                if (s.isCustomer)
                  IconButton.filledTonal(
                    icon: Icon(fav ? Icons.favorite : Icons.favorite_border, color: fav ? cs.error : null),
                    onPressed: () => s.toggleFavorite('store:${store.id}'),
                  ),
                const SizedBox(width: 8),
              ],
              flexibleSpace: FlexibleSpaceBar(
                  background: Hero(tag: 'store-${store.id}', child: StoreImage(photoUrl: store.photoUrl, type: store.type, height: 240))),
            ),
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 14, 16, 0),
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Row(children: [
                    Expanded(child: Text(s.storeName(store), style: tt.headlineSmall?.copyWith(fontWeight: FontWeight.w900))),
                    Stars(store.rating, store.ratingCount),
                  ]),
                  if ((s.ar ? store.taglineAr : store.tagline) != null)
                    Text((s.ar ? store.taglineAr : store.tagline)!, style: tt.bodyMedium?.copyWith(color: cs.onSurfaceVariant)),
                  const SizedBox(height: 8),
                  Wrap(spacing: 16, runSpacing: 6, children: [
                    if (s.zoneOf(s.zone) != null)
                      _Info(Icons.delivery_dining,
                          '${s.t('deliveryInfo')} ${s.zoneOf(s.zone)!.etaMinutes + 15} ${s.t('min')} · ${s.money(s.zoneOf(s.zone)!.fee)}'),
                    _Info(Icons.restaurant_menu, '${d.sections.fold<int>(0, (n, sec) => n + sec.products.length)} ${s.t('itemsCount')}'),
                    if (store.opens != null && store.closes != null)
                      _Info(Icons.schedule, '${clock(s, store.opens!)} – ${clock(s, store.closes!)}'),
                    if (d.address != null) _Info(Icons.location_on_outlined, d.address!),
                    if (d.phone != null) InkWell(onTap: () => callPhone(d.phone!), child: _Info(Icons.call_outlined, d.phone!)),
                  ]),
                  if (!store.orderable)
                    Container(
                      margin: const EdgeInsets.only(top: 12),
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(color: cs.errorContainer, borderRadius: BorderRadius.circular(12)),
                      child: Row(children: [
                        Icon(Icons.info_outline, color: cs.onErrorContainer),
                        const SizedBox(width: 8),
                        Expanded(child: Text(s.t('storeClosedBanner'), style: TextStyle(color: cs.onErrorContainer))),
                      ]),
                    ),
                  const SizedBox(height: 12),
                  _Reviews(reviews),
                  const SizedBox(height: 12),
                  TextField(
                    onChanged: (v) => setState(() => query = v),
                    decoration: InputDecoration(hintText: s.t('menuSearch'), prefixIcon: const Icon(Icons.search)),
                  ),
                ]),
              ),
            ),
            if (d.sections.length > 1)
              SliverToBoxAdapter(
                child: SizedBox(
                  height: 54,
                  child: ListView(
                    padding: const EdgeInsets.fromLTRB(16, 10, 16, 0),
                    scrollDirection: Axis.horizontal,
                    children: [
                      Padding(
                        padding: const EdgeInsetsDirectional.only(end: 8),
                        child: ChoiceChip(label: Text(s.t('allTypes')), selected: section == null, onSelected: (_) => setState(() => section = null)),
                      ),
                      for (final sec in d.sections)
                        Padding(
                          padding: const EdgeInsetsDirectional.only(end: 8),
                          child: ChoiceChip(
                              label: Text(sectionName(s, sec.code)),
                              selected: section == sec.code,
                              onSelected: (_) => setState(() => section = sec.code)),
                        ),
                    ],
                  ),
                ),
              ),
            for (final sec in sections) ...[
              SliverToBoxAdapter(child: SectionTitle(sectionName(s, sec.code))),
              SliverList.separated(
                itemCount: sec.products.length,
                separatorBuilder: (_, _) => const SizedBox(height: 10),
                itemBuilder: (c, i) => FadeSlideIn(
                  index: i,
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    child: _ProductTile(
                      product: sec.products[i],
                      enabled: store.orderable && sec.products[i].available && s.isCustomer,
                      onAdd: () => _add(s, store, sec.products[i]),
                    ),
                  ),
                ),
              ),
            ],
            if (sections.isEmpty) SliverToBoxAdapter(child: EmptyState(Icons.search_off, s.t('empty'))),
            const SliverToBoxAdapter(child: SizedBox(height: 100)),
          ]),
          bottomNavigationBar: inCart == 0
              ? null
              : SafeArea(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(16, 4, 16, 10),
                    child: FilledButton(
                      onPressed: () {
                        context.read<AppState>().goTab(2);
                        Navigator.of(context).popUntil((r) => r.isFirst);
                      },
                      child: Row(children: [
                        CircleAvatar(radius: 14, backgroundColor: cs.onPrimary, child: Text('$inCart', style: TextStyle(color: cs.primary, fontSize: 13))),
                        Expanded(child: Text(s.t('viewCart'), textAlign: TextAlign.center)),
                        Text(s.money(s.cartSubtotal)),
                      ]),
                    ),
                  ),
                ),
        );
      },
    );
  }
}

/// A dish on its own page, like the big apps: big photo, both descriptions, quantity, a note and one Add button.
/// Returns (quantity, note) to the store page, which puts it in the cart.
class _DishPage extends StatefulWidget {
  final Product product;
  final Store store;
  final String note;
  const _DishPage({required this.product, required this.store, required this.note});
  @override
  State<_DishPage> createState() => _DishPageState();
}

class _DishPageState extends State<_DishPage> {
  int qty = 1;
  late final note = TextEditingController(text: widget.note);

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    final p = widget.product;
    final url = imageUrl(p.photoUrl);
    final enabled = widget.store.orderable && p.available && s.isCustomer;
    return Scaffold(
      body: CustomScrollView(slivers: [
        SliverAppBar(
          pinned: true,
          expandedHeight: 300,
          flexibleSpace: FlexibleSpaceBar(
            background: url == null
                ? Container(color: cs.primaryContainer, child: Icon(Icons.restaurant, size: 80, color: cs.primary))
                : Image.network(url, fit: BoxFit.cover),
          ),
        ),
        SliverToBoxAdapter(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 18, 20, 24),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(s.productName(p), style: tt.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
              const SizedBox(height: 4),
              Text(s.money(p.price), style: tt.titleLarge?.copyWith(color: cs.primary, fontWeight: FontWeight.w900)),
              const SizedBox(height: 6),
              Text(s.storeName(widget.store), style: tt.bodyMedium?.copyWith(color: cs.onSurfaceVariant)),
              if (s.productDesc(p) != null) ...[
                const SizedBox(height: 18),
                Text(s.t('aboutDish'), style: tt.titleMedium?.copyWith(fontWeight: FontWeight.w800)),
                const SizedBox(height: 4),
                Text(s.productDesc(p)!, style: tt.bodyLarge),
              ],
              if (!p.available)
                Padding(
                  padding: const EdgeInsets.only(top: 12),
                  child: Text(s.t('soldOut'), style: TextStyle(color: cs.error, fontWeight: FontWeight.w800)),
                ),
              const SizedBox(height: 22),
              Text(s.t('quantity'), style: tt.titleMedium?.copyWith(fontWeight: FontWeight.w800)),
              const SizedBox(height: 8),
              Row(children: [
                IconButton.filledTonal(onPressed: qty > 1 ? () => setState(() => qty--) : null, icon: const Icon(Icons.remove)),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 22),
                  child: Text('$qty', style: const TextStyle(fontSize: 24, fontWeight: FontWeight.w900)),
                ),
                IconButton.filledTonal(onPressed: qty < 99 ? () => setState(() => qty++) : null, icon: const Icon(Icons.add)),
              ]),
              const SizedBox(height: 18),
              TextField(controller: note, maxLength: 200, decoration: InputDecoration(labelText: s.t('note'))),
            ]),
          ),
        ),
      ]),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 4, 16, 12),
          child: FilledButton.icon(
            icon: const Icon(Icons.add_shopping_cart),
            onPressed: enabled ? () => Navigator.pop(context, (qty, note.text.trim())) : null,
            label: Text('${s.t('addToCart')} · ${s.money(p.price * qty)}'),
          ),
        ),
      ),
    );
  }
}

/// The store's latest reviews in a row of cards, or a line inviting the first one.
class _Reviews extends StatelessWidget {
  final Future<List<dynamic>> reviews;
  const _Reviews(this.reviews);
  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    return FutureBuilder<List<dynamic>>(
      future: reviews,
      builder: (c, snap) {
        final list = snap.data ?? const [];
        return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(s.t('reviewsTitle'), style: tt.titleMedium?.copyWith(fontWeight: FontWeight.w800)),
          const SizedBox(height: 6),
          if (list.isEmpty)
            Text(s.t('noReviews'), style: tt.bodyMedium?.copyWith(color: cs.onSurfaceVariant))
          else
            SizedBox(
              height: 110,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                itemCount: list.length,
                separatorBuilder: (_, _) => const SizedBox(width: 10),
                itemBuilder: (c, i) {
                  final r = list[i];
                  final rating = (r['rating'] as num?)?.toInt() ?? 0;
                  return Container(
                    width: 230,
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(color: cs.surfaceContainerLow, borderRadius: BorderRadius.circular(14)),
                    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      Row(children: [for (var k = 0; k < 5; k++) Icon(k < rating ? Icons.star : Icons.star_border, size: 18, color: cs.tertiary)]),
                      const SizedBox(height: 6),
                      Expanded(
                        child: Text((r['comment'] ?? '').toString().isEmpty ? '—' : r['comment'],
                            maxLines: 3, overflow: TextOverflow.ellipsis),
                      ),
                    ]),
                  );
                },
              ),
            ),
        ]);
      },
    );
  }
}

/// An icon and a line of store info (time, address, phone…).
class _Info extends StatelessWidget {
  final IconData icon;
  final String text;
  const _Info(this.icon, this.text);
  @override
  Widget build(BuildContext context) => Row(mainAxisSize: MainAxisSize.min, children: [
        Icon(icon, size: 18, color: Theme.of(context).colorScheme.primary),
        const SizedBox(width: 6),
        Flexible(child: Text(text)),
      ]);
}

class _ProductTile extends StatelessWidget {
  final Product product;
  final bool enabled;
  final VoidCallback onAdd;
  const _ProductTile({required this.product, required this.enabled, required this.onAdd});

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    final qty = s.qtyOf(product.id);
    return Card(
      child: InkWell(
        onTap: enabled ? onAdd : null,
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(s.productName(product), style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
                if (s.productDesc(product) != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 2),
                    child: Text(s.productDesc(product)!, maxLines: 2, overflow: TextOverflow.ellipsis, style: TextStyle(color: cs.onSurfaceVariant)),
                  ),
                const SizedBox(height: 6),
                Row(children: [
                  Text(s.money(product.price), style: TextStyle(color: cs.primary, fontWeight: FontWeight.w900, fontSize: 16)),
                  const SizedBox(width: 8),
                  if (product.large) Text('🛺', style: TextStyle(color: cs.tertiary)),
                  if (!product.available)
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                      decoration: BoxDecoration(color: cs.errorContainer, borderRadius: BorderRadius.circular(8)),
                      child: Text(s.t('soldOut'), style: TextStyle(color: cs.onErrorContainer, fontSize: 12)),
                    ),
                ]),
              ]),
            ),
            const SizedBox(width: 10),
            Stack(clipBehavior: Clip.none, children: [
              ProductImage(product.photoUrl),
              PositionedDirectional(
                bottom: -8,
                end: -6,
                child: qty > 0
                    ? Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                        decoration: BoxDecoration(color: cs.primary, borderRadius: BorderRadius.circular(20)),
                        child: Text('×$qty', style: TextStyle(color: cs.onPrimary, fontWeight: FontWeight.w900)),
                      )
                    : IconButton.filled(
                        visualDensity: VisualDensity.compact,
                        onPressed: enabled ? onAdd : null,
                        icon: const Icon(Icons.add),
                      ),
              ),
            ]),
          ]),
        ),
      ),
    );
  }
}

/// Menu section codes (MEALS, DRINKS…) shown in the user's language.
String sectionName(AppState s, String code) {
  const names = {
    'MEALS': ['وجبات', 'Meals'],
    'SANDWICHES': ['ساندوتشات', 'Sandwiches'],
    'SOUPS': ['شوربة', 'Soups'],
    'SALADS': ['سلطات ومقبلات', 'Salads & dips'],
    'APPETIZERS': ['مقبلات ساخنة', 'Hot appetizers'],
    'GRILL_PAN': ['الجريل', 'Grill pan'],
    'TAGINES': ['طواجن', 'Tagines'],
    'TRAYS': ['صواني وشوايات', 'Trays & platters'],
    'PASTA_RICE': ['مكرونة وأرز', 'Pasta & rice'],
    'PIZZA': ['بيتزا', 'Pizza'],
    'FATEER': ['فطير', 'Fateer'],
    'GRILLS': ['مشويات', 'Grills'],
    'SIDES': ['أطباق جانبية', 'Sides'],
    'DRINKS': ['مشروبات', 'Drinks'],
    'DESSERTS': ['حلويات', 'Desserts'],
    'BREAKFAST': ['فطار', 'Breakfast'],
    'OFFERS': ['عروض', 'Offers'],
    'MEDICINE': ['أدوية', 'Medicine'],
    'CARE': ['عناية شخصية', 'Personal care'],
    'BABY': ['أطفال', 'Baby'],
    'DAIRY': ['ألبان', 'Dairy'],
    'BAKERY': ['مخبوزات', 'Bakery'],
    'VEGETABLES': ['خضار وفاكهة', 'Fruit & veg'],
    'CLEANING': ['منظفات', 'Cleaning'],
    'GAS': ['أنابيب', 'Gas'],
    'OTHER': ['حاجات تانية', 'Other'],
  };
  final n = names[code];
  if (n != null) return n[s.ar ? 0 : 1];
  final words = code.toLowerCase().replaceAll('_', ' ');
  return words.isEmpty ? code : words[0].toUpperCase() + words.substring(1);
}

/// "14:30" → "2:30 م" (Arabic) or "2:30 PM".
String clock(AppState s, String hhmm) {
  final parts = hhmm.split(':');
  if (parts.length < 2) return hhmm;
  final h = int.tryParse(parts[0]) ?? 0;
  final m = parts[1].padLeft(2, '0').substring(0, 2);
  final pm = h >= 12;
  final h12 = h % 12 == 0 ? 12 : h % 12;
  return '$h12:$m ${s.ar ? (pm ? 'م' : 'ص') : (pm ? 'PM' : 'AM')}';
}
