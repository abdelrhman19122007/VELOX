import 'dart:async';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/app_state.dart';
import '../../core/models.dart';
import '../../widgets/common.dart';
import '../shared/inbox_screen.dart';
import '../shared/settings_screen.dart';
import 'errand_screen.dart';
import 'search_screen.dart';
import 'store_screen.dart';

/// Home: area picker, search, offers, "bring me anything", store types and the store list.
/// Store cards show what people decide on (rating, open or not, delivery time and fee), like the big apps.
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});
  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  String type = 'ALL';
  String cuisine = 'ALL';
  bool openOnly = false, favOnly = false;

  static const _types = ['FOOD', 'GROCERY', 'PHARMACY', 'CLOTHES', 'TECH', 'OTHER'];
  static const _deals = ['VELOX10', 'FREESHIP', 'WELCOME'];

  List<Store> _filtered(AppState s) {
    final list = s.stores.where((st) {
      if (type != 'ALL' && st.type != type) return false;
      if (cuisine != 'ALL' && st.cuisine != cuisine) return false;
      if (openOnly && !st.orderable) return false;
      if (favOnly && !s.favorites.contains('store:${st.id}')) return false;
      return true;
    }).toList();
    // open stores first, then by rating
    list.sort((a, b) {
      final o = (b.orderable ? 1 : 0) - (a.orderable ? 1 : 0);
      return o != 0 ? o : (b.rating ?? 0).compareTo(a.rating ?? 0);
    });
    return list;
  }

  Future<void> _pickZone(AppState s) async {
    final z = await showModalBottomSheet<String>(
      context: context,
      builder: (c) => SafeArea(
        child: ListView(shrinkWrap: true, children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
            child: Text(s.t('pickZone'), style: Theme.of(c).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800)),
          ),
          for (final z in s.zones)
            ListTile(
              leading: Icon(z.kind == 'CITY' ? Icons.location_city : Icons.holiday_village_outlined),
              title: Text(s.ar ? z.nameAr : z.nameEn),
              subtitle: Text('${s.t('deliveryFee')} ${s.money(z.fee)} · ${z.etaMinutes} ${s.t('min')}'),
              trailing: z.code == s.zone ? Icon(Icons.check_circle, color: Theme.of(c).colorScheme.primary) : null,
              onTap: () => Navigator.pop(c, z.code),
            ),
        ]),
      ),
    );
    if (z != null) s.setZone(z);
  }

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    if (s.stores.isEmpty && s.zones.isEmpty) {
      return Scaffold(
        body: s.catalogError == null
            ? const Center(child: CircularProgressIndicator())
            : EmptyState(Icons.cloud_off, s.catalogError!,
                action: Wrap(spacing: 8, children: [
                  FilledButton(onPressed: s.loadCatalog, child: Text(s.t('retry'))),
                  OutlinedButton(onPressed: () => showServerDialog(context), child: Text(s.t('server'))),
                ])),
      );
    }
    final list = _filtered(s);
    final lastDelivered = s.myOrders.where((o) => o.status == 'DELIVERED' && o.store != null).firstOrNull;
    return Scaffold(
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: s.loadCatalog,
          child: CustomScrollView(slivers: [
            SliverAppBar(
              floating: true,
              titleSpacing: 16,
              toolbarHeight: 64,
              title: InkWell(
                onTap: () => _pickZone(s),
                borderRadius: BorderRadius.circular(12),
                child: Row(mainAxisSize: MainAxisSize.min, children: [
                  Icon(Icons.location_on, color: cs.primary),
                  const SizedBox(width: 6),
                  Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisSize: MainAxisSize.min, children: [
                    Text(s.t('deliverTo'), style: tt.labelSmall?.copyWith(color: cs.onSurfaceVariant)),
                    Text(s.zoneName(s.zone), style: tt.titleMedium?.copyWith(fontWeight: FontWeight.w900)),
                  ]),
                  const Icon(Icons.keyboard_arrow_down),
                ]),
              ),
              actions: [
                TextButton(onPressed: s.toggleLang, child: Text(s.t('switchLang'))),
                IconButton(
                  tooltip: s.t('notifications'),
                  onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const InboxScreen())),
                  icon: Badge(
                    isLabelVisible: s.unread > 0,
                    label: Text('${s.unread}'),
                    child: const Icon(Icons.notifications_outlined),
                  ),
                ),
                const SizedBox(width: 6),
              ],
            ),
            // greeting
            SliverToBoxAdapter(
              child: FadeSlideIn(
                index: 0,
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(16, 4, 16, 10),
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Text('${s.t('hello')}${s.user == null ? '' : ' ${s.user!.name.split(' ').first}'}',
                        style: tt.titleMedium?.copyWith(color: cs.onSurfaceVariant)),
                    Text(s.t('craving'), style: tt.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
                    const SizedBox(height: 4),
                    Text(s.t('heroSub'), style: tt.bodyMedium?.copyWith(color: cs.onSurfaceVariant)),
                  ]),
                ),
              ),
            ),
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 4, 16, 0),
                child: InkWell(
                  borderRadius: BorderRadius.circular(14),
                  onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const SearchScreen())),
                  child: IgnorePointer(
                    child: TextField(decoration: InputDecoration(hintText: s.t('searchPh'), prefixIcon: const Icon(Icons.search))),
                  ),
                ),
              ),
            ),
            // featured stores with photos
            if (s.stores.any((st) => st.photoUrl != null)) ...[
              SliverToBoxAdapter(child: SectionTitle(s.t('featured'))),
              SliverToBoxAdapter(child: _Featured(s.stores.where((st) => st.photoUrl != null).toList())),
            ],
            // categories, like the big apps: a row of circles that filter the stores below
            if (s.stores.map((st) => st.cuisine).toSet().length > 1) ...[
              SliverToBoxAdapter(child: SectionTitle(s.t('categories'))),
              SliverToBoxAdapter(
                child: SizedBox(
                  height: 104,
                  child: ListView(
                    padding: const EdgeInsets.symmetric(horizontal: 12),
                    scrollDirection: Axis.horizontal,
                    children: [
                      for (final c in ['ALL', ...{for (final st in s.stores) st.cuisine}])
                        _CuisineCircle(
                          label: s.t('cuisine_$c') == 'cuisine_$c' ? c : s.t('cuisine_$c'),
                          icon: _cuisineIcon(c),
                          selected: cuisine == c,
                          onTap: () => setState(() => cuisine = c),
                        ),
                    ],
                  ),
                ),
              ),
            ],
            // offers
            SliverToBoxAdapter(child: SectionTitle(s.t('offers'))),
            SliverToBoxAdapter(child: _SubTitle(s.t('offersSub'))),
            SliverToBoxAdapter(
              child: SizedBox(
                height: 138,
                child: ListView.separated(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  scrollDirection: Axis.horizontal,
                  itemCount: _deals.length,
                  separatorBuilder: (_, _) => const SizedBox(width: 10),
                  itemBuilder: (c, i) => _deal(s, _deals[i], [cs.primary, cs.secondary, cs.tertiary][i]),
                ),
              ),
            ),
            // order again
            if (lastDelivered != null)
              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(16, 14, 16, 0),
                  child: Card(
                    child: ListTile(
                      leading: Icon(Icons.replay, color: cs.primary),
                      title: Text(s.t('orderAgain'), style: const TextStyle(fontWeight: FontWeight.w800)),
                      subtitle: Text('${s.ar ? lastDelivered.store!.nameAr : lastDelivered.store!.name} · ${s.money(lastDelivered.total)}'),
                      trailing: const Icon(Icons.chevron_right),
                      onTap: () async {
                        final missing = await busy(context, () => s.reorder(lastDelivered));
                        if (missing != null && context.mounted) context.read<AppState>().goTab(2);
                      },
                    ),
                  ),
                ),
              ),
            // errand
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 14, 16, 0),
                child: Material(
                  color: cs.secondaryContainer,
                  borderRadius: BorderRadius.circular(18),
                  child: InkWell(
                    borderRadius: BorderRadius.circular(18),
                    onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const ErrandScreen())),
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Row(children: [
                        CircleAvatar(radius: 24, backgroundColor: cs.secondary, child: Icon(Icons.shopping_basket, color: cs.onSecondary)),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                            Text(s.t('errandTitle'), style: tt.titleSmall?.copyWith(fontWeight: FontWeight.w800, color: cs.onSecondaryContainer)),
                            const SizedBox(height: 2),
                            Text(s.t('errandSub'), style: tt.bodySmall?.copyWith(color: cs.onSecondaryContainer)),
                          ]),
                        ),
                        Icon(Icons.chevron_right, color: cs.onSecondaryContainer),
                      ]),
                    ),
                  ),
                ),
              ),
            ),
            // types + filters
            if (s.stores.any((st) => st.orderable && st.photoUrl != null)) ...[
              SliverToBoxAdapter(child: SectionTitle(s.t('popularTitle'))),
              SliverToBoxAdapter(child: _SubTitle(s.t('popularSub'))),
              SliverToBoxAdapter(child: _PopularDishes(s.stores.firstWhere((st) => st.orderable && st.photoUrl != null))),
            ],
            SliverToBoxAdapter(child: SectionTitle(s.t('stores'))),
            SliverToBoxAdapter(child: _SubTitle(s.t('storesSub'))),
            SliverToBoxAdapter(
              child: SizedBox(
                height: 44,
                child: ListView(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  scrollDirection: Axis.horizontal,
                  children: [
                    _typeChip(s, 'ALL', Icons.apps),
                    for (final t in _types)
                      if (s.stores.any((st) => st.type == t)) _typeChip(s, t, StoreImage.iconFor(t)),
                  ],
                ),
              ),
            ),
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 4),
                child: Wrap(spacing: 8, children: [
                  FilterChip(label: Text(s.t('openNow')), selected: openOnly, onSelected: (v) => setState(() => openOnly = v)),
                  if (s.favorites.any((f) => f.startsWith('store:')))
                    FilterChip(
                      avatar: const Icon(Icons.favorite, size: 18),
                      label: Text(s.t('favorites')),
                      selected: favOnly,
                      onSelected: (v) => setState(() => favOnly = v),
                    ),
                ]),
              ),
            ),
            if (list.isEmpty)
              SliverToBoxAdapter(child: EmptyState(Icons.storefront, s.t('noStores')))
            else
              SliverPadding(
                padding: const EdgeInsets.fromLTRB(16, 4, 16, 24),
                sliver: SliverList.separated(
                  itemCount: list.length,
                  separatorBuilder: (_, _) => const SizedBox(height: 14),
                  itemBuilder: (c, i) => FadeSlideIn(index: i, child: StoreCard(list[i])),
                ),
              ),
            const SliverToBoxAdapter(child: _HowItWorks()),
          ]),
        ),
      ),
    );
  }

  Widget _typeChip(AppState s, String t, IconData icon) => Padding(
        padding: const EdgeInsetsDirectional.only(end: 8),
        child: ChoiceChip(
          avatar: Icon(icon, size: 18),
          label: Text(t == 'ALL' ? s.t('allTypes') : s.t('type_$t')),
          selected: type == t,
          onSelected: (_) => setState(() => type = t),
        ),
      );

  Widget _deal(AppState s, String code, Color color) {
    return Container(
      width: 270,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(18)),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(code, style: const TextStyle(color: Colors.white70, fontWeight: FontWeight.w800, letterSpacing: 1)),
        const SizedBox(height: 4),
        Expanded(
          child: Text(s.t('deal_$code'),
              maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w800, fontSize: 16)),
        ),
        Align(
          alignment: AlignmentDirectional.centerEnd,
          child: FilledButton.tonal(
            style: FilledButton.styleFrom(minimumSize: const Size(0, 34), padding: const EdgeInsets.symmetric(horizontal: 14)),
            onPressed: () {
              s.setPromo(code);
              showInfo(context, s.t('codeSaved'));
            },
            child: Text(s.t('useCode')),
          ),
        ),
      ]),
    );
  }
}

IconData _cuisineIcon(String c) => switch (c) {
      'ALL' => Icons.apps,
      'GRILLS' => Icons.outdoor_grill,
      'CAFE' => Icons.local_cafe,
      'PIZZA' => Icons.local_pizza,
      'SWEETS' => Icons.icecream,
      'FASTFOOD' => Icons.lunch_dining,
      'SEAFOOD' => Icons.set_meal,
      'PHARMACY' => Icons.local_pharmacy,
      'GROCERY' => Icons.local_grocery_store,
      _ => Icons.restaurant,
    };

/// One category circle; the chosen one is filled with the brand colour.
class _CuisineCircle extends StatelessWidget {
  final String label;
  final IconData icon;
  final bool selected;
  final VoidCallback onTap;
  const _CuisineCircle({required this.label, required this.icon, required this.selected, required this.onTap});
  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return InkWell(
      borderRadius: BorderRadius.circular(16),
      onTap: onTap,
      child: SizedBox(
        width: 80,
        child: Column(children: [
          AnimatedContainer(
            duration: const Duration(milliseconds: 200),
            width: 60,
            height: 60,
            decoration: BoxDecoration(
              color: selected ? cs.primary : cs.primaryContainer,
              shape: BoxShape.circle,
            ),
            child: Icon(icon, color: selected ? cs.onPrimary : cs.primary, size: 28),
          ),
          const SizedBox(height: 6),
          Text(label, maxLines: 1, overflow: TextOverflow.ellipsis,
              style: TextStyle(fontWeight: selected ? FontWeight.w900 : FontWeight.w600, fontSize: 13)),
        ]),
      ),
    );
  }
}

/// A grey line under a section title.
class _SubTitle extends StatelessWidget {
  final String text;
  const _SubTitle(this.text);
  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
        child: Text(text, style: Theme.of(context).textTheme.bodyMedium?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant)),
      );
}

/// Real dishes with photos and prices from an open store's menu; tapping opens the store.
class _PopularDishes extends StatelessWidget {
  final Store store;
  const _PopularDishes(this.store);
  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    return FutureBuilder<StoreDetail>(
      future: s.storeDetail(store.id),
      builder: (c, snap) {
        if (!snap.hasData) return const SizedBox(height: 8);
        // one dish from each section, so the row shows the whole menu's range
        final dishes = [for (final sec in snap.data!.sections) if (sec.products.isNotEmpty) sec.products.first].take(8).toList();
        return SizedBox(
          height: 206,
          child: ListView.separated(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            scrollDirection: Axis.horizontal,
            itemCount: dishes.length,
            separatorBuilder: (_, _) => const SizedBox(width: 12),
            itemBuilder: (c, i) {
              final p = dishes[i];
              return FadeSlideIn(
                index: i,
                child: SizedBox(
                  width: 150,
                  child: Card(
                    child: InkWell(
                      onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => StoreScreen(storeId: store.id))),
                      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        ProductImage(p.photoUrl, size: 150),
                        Padding(
                          padding: const EdgeInsets.fromLTRB(10, 8, 10, 0),
                          child: Text(s.productName(p), maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w800)),
                        ),
                        Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 10),
                          child: Text(s.money(p.price), style: TextStyle(color: cs.primary, fontWeight: FontWeight.w900)),
                        ),
                      ]),
                    ),
                  ),
                ),
              );
            },
          ),
        );
      },
    );
  }
}

/// "How it works" and "why VELOX": short reassurance text under the store list, like the big apps.
class _HowItWorks extends StatelessWidget {
  const _HowItWorks();
  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    Widget step(IconData icon, String title, String sub) => Expanded(
          child: Column(children: [
            CircleAvatar(radius: 24, backgroundColor: cs.primaryContainer, child: Icon(icon, color: cs.primary)),
            const SizedBox(height: 8),
            Text(title, textAlign: TextAlign.center, style: tt.titleSmall?.copyWith(fontWeight: FontWeight.w800)),
            const SizedBox(height: 2),
            Text(sub, textAlign: TextAlign.center, style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant)),
          ]),
        );
    Widget why(String text) => Padding(
          padding: const EdgeInsets.symmetric(vertical: 4),
          child: Row(children: [
            Icon(Icons.check_circle, size: 20, color: cs.secondary),
            const SizedBox(width: 8),
            Expanded(child: Text(text)),
          ]),
        );
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 28),
      child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Text(s.t('howTitle'), style: tt.titleLarge?.copyWith(fontWeight: FontWeight.w900)),
        const SizedBox(height: 14),
        Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
          step(Icons.restaurant_menu, s.t('how1'), s.t('how1Sub')),
          step(Icons.payments_outlined, s.t('how2'), s.t('how2Sub')),
          step(Icons.delivery_dining, s.t('how3'), s.t('how3Sub')),
        ]),
        const SizedBox(height: 22),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(s.t('whyTitle'), style: tt.titleMedium?.copyWith(fontWeight: FontWeight.w900)),
              const SizedBox(height: 6),
              why(s.t('why1')),
              why(s.t('why2')),
              why(s.t('why3')),
            ]),
          ),
        ),
        const SizedBox(height: 18),
        Text(s.t('footer'), textAlign: TextAlign.center, style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant)),
      ]),
    );
  }
}

/// Big photo cards that slide by themselves, for stores that have a photo.
class _Featured extends StatefulWidget {
  final List<Store> stores;
  const _Featured(this.stores);
  @override
  State<_Featured> createState() => _FeaturedState();
}

class _FeaturedState extends State<_Featured> {
  final page = PageController(viewportFraction: 0.88);
  Timer? timer;
  int current = 0;

  @override
  void initState() {
    super.initState();
    if (widget.stores.length > 1) {
      timer = Timer.periodic(const Duration(seconds: 4), (_) {
        if (!page.hasClients) return;
        page.animateToPage((current + 1) % widget.stores.length,
            duration: const Duration(milliseconds: 600), curve: Curves.easeOutCubic);
      });
    }
  }

  @override
  void dispose() {
    timer?.cancel();
    page.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    return Column(children: [
      SizedBox(
        height: 190,
        child: PageView.builder(
          controller: page,
          itemCount: widget.stores.length,
          onPageChanged: (i) => setState(() => current = i),
          itemBuilder: (c, i) {
            final st = widget.stores[i];
            final tagline = s.ar ? st.taglineAr : st.tagline;
            return AnimatedScale(
              scale: i == current ? 1 : 0.94,
              duration: const Duration(milliseconds: 300),
              child: GestureDetector(
                onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => StoreScreen(storeId: st.id))),
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 6),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(20),
                    child: Stack(fit: StackFit.expand, children: [
                      StoreImage(photoUrl: st.photoUrl, type: st.type, height: 190),
                      const DecoratedBox(
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                              begin: Alignment.topCenter, end: Alignment.bottomCenter, colors: [Colors.transparent, Colors.black87]),
                        ),
                      ),
                      PositionedDirectional(
                        start: 16,
                        end: 16,
                        bottom: 14,
                        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                          Text(s.storeName(st), style: const TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.w900)),
                          if ((tagline ?? '').isNotEmpty)
                            Text(tagline!, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(color: Colors.white70)),
                        ]),
                      ),
                    ]),
                  ),
                ),
              ),
            );
          },
        ),
      ),
      if (widget.stores.length > 1)
        Padding(
          padding: const EdgeInsets.only(top: 8),
          child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
            for (var i = 0; i < widget.stores.length; i++)
              AnimatedContainer(
                duration: const Duration(milliseconds: 250),
                margin: const EdgeInsets.symmetric(horizontal: 3),
                width: i == current ? 20 : 7,
                height: 7,
                decoration: BoxDecoration(color: i == current ? cs.primary : cs.outlineVariant, borderRadius: BorderRadius.circular(4)),
              ),
          ]),
        ),
    ]);
  }
}

/// A store in a list: photo (or type icon), name, rating, open/closed, hours, delivery time and fee.
class StoreCard extends StatelessWidget {
  final Store store;
  const StoreCard(this.store, {super.key});

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    final zone = s.zoneOf(s.zone);
    final fav = s.favorites.contains('store:${store.id}');
    final tagline = s.ar ? store.taglineAr : store.tagline;
    return Card(
      child: InkWell(
        onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => StoreScreen(storeId: store.id))),
        child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          Stack(children: [
            Hero(
              tag: 'store-${store.id}',
              child: Opacity(opacity: store.orderable ? 1 : 0.55, child: StoreImage(photoUrl: store.photoUrl, type: store.type, height: 150)),
            ),
            PositionedDirectional(
              top: 10,
              start: 10,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: store.orderable ? cs.secondary : cs.onSurface.withValues(alpha: 0.75),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text(store.orderable ? s.t('open') : (store.openNow ? s.t('busy') : s.t('closed')),
                    style: TextStyle(color: store.orderable ? cs.onSecondary : cs.surface, fontWeight: FontWeight.w700, fontSize: 12.5)),
              ),
            ),
            if (s.isCustomer)
              PositionedDirectional(
                top: 4,
                end: 4,
                child: IconButton.filledTonal(
                  icon: Icon(fav ? Icons.favorite : Icons.favorite_border, color: fav ? cs.error : null),
                  onPressed: () => s.toggleFavorite('store:${store.id}'),
                ),
              ),
          ]),
          Padding(
            padding: const EdgeInsets.fromLTRB(14, 10, 14, 12),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Row(children: [
                Expanded(child: Text(s.storeName(store), style: tt.titleMedium?.copyWith(fontWeight: FontWeight.w900))),
                Stars(store.rating, store.ratingCount),
              ]),
              if ((tagline ?? '').isNotEmpty)
                Text(tagline!, maxLines: 1, overflow: TextOverflow.ellipsis, style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant)),
              const SizedBox(height: 6),
              Wrap(spacing: 14, runSpacing: 4, children: [
                _info(Icons.category_outlined, s.t('type_${store.type}'), cs),
                if (zone != null) _info(Icons.schedule, '${zone.etaMinutes + 15} ${s.t('min')}', cs),
                if (zone != null) _info(Icons.two_wheeler, s.money(zone.fee), cs),
                if (store.opens != null && store.closes != null) _info(Icons.access_time, '${clock(s, store.opens!)} – ${clock(s, store.closes!)}', cs),
              ]),
            ]),
          ),
        ]),
      ),
    );
  }

  Widget _info(IconData icon, String text, ColorScheme cs) => Row(mainAxisSize: MainAxisSize.min, children: [
        Icon(icon, size: 16, color: cs.onSurfaceVariant),
        const SizedBox(width: 4),
        Text(text, style: TextStyle(color: cs.onSurfaceVariant, fontSize: 13)),
      ]);
}
