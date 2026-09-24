import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../core/locale.dart';
import '../models/product.dart';
import '../services/cart_state.dart';
import '../services/catalog_service.dart';
import '../theme/app_theme.dart';
import '../widgets/glass_card.dart';
import '../widgets/product_card.dart';
import '../widgets/section_header.dart';
import 'browse_screen.dart';
import 'cart_screen.dart';
import 'product/product_detail_sheet.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final _searchCtrl = TextEditingController();
  List<Product> _recommended = [];

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final products = await CatalogService.instance.load();
    if (!mounted) return;
    setState(() {
      // mix food + fashion + electronics for a dynamic rail
      final foods = products.where((p) => p.isFood).toList()..shuffle();
      final rest = products.where((p) => !p.isFood).toList()..shuffle();
      _recommended = [...foods.take(6), ...rest.take(2)]..shuffle();
    });
  }

  void _copyPromo() {
    Clipboard.setData(const ClipboardData(text: 'VELOX30'));
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(
        content: Text('${AppLocale.tr('home.copied')} — VELOX30'),
        duration: const Duration(seconds: 1),
      ));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: CustomScrollView(
        slivers: [
          SliverAppBar(
            pinned: true,
            backgroundColor: AppColors.canvas,
            leadingWidth: 0,
            title: Row(
              children: [
                Image.asset(
                  'assets/images/velox-logo.jpeg',
                  width: 30,
                  height: 30,
                  fit: BoxFit.cover,
                  errorBuilder: (_, __, ___) =>
                      const Icon(Icons.bolt, color: AppColors.primary),
                ),
                const SizedBox(width: 8),
                const Text('VELOX',
                    style: TextStyle(
                        fontWeight: FontWeight.w800, letterSpacing: 2)),
              ],
            ),
            actions: [
              _LocationChip(),
              const SizedBox(width: 8),
              _CartIconButton(),
              const SizedBox(width: 6),
            ],
          ),
          SliverToBoxAdapter(child: _buildHero(context)),
          SliverToBoxAdapter(child: _buildPromoCard(context)),
          SliverToBoxAdapter(child: _buildPillars(context)),
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 4),
              child: SectionHeader(
                label: AppLocale.tr('home.recommended').toUpperCase(),
                title: AppLocale.tr('home.recommended'),
                actionLabel: AppLocale.tr('home.viewAll'),
                onAction: () => Navigator.of(context).push(
                  MaterialPageRoute(
                      builder: (_) => const BrowseScreen(initial: 'all')),
                ),
              ),
            ),
          ),
          SliverPadding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
            sliver: SliverGrid(
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 2,
                mainAxisSpacing: 12,
                crossAxisSpacing: 12,
                childAspectRatio: 0.72,
              ),
              delegate: SliverChildBuilderDelegate(
                (context, i) {
                  final p = _recommended[i];
                  return ProductCard(
                    product: p,
                    onTap: () => showProductDetail(context, p),
                  );
                },
                childCount: _recommended.length,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildHero(BuildContext context) {
    return Stack(
      children: [
        const AmbientGlow(color: AppColors.primary, alignment: Alignment.topLeft, size: 320),
        const AmbientGlow(color: AppColors.secondary, alignment: Alignment.bottomRight, size: 280),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Velocity status pill
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(
                  color: AppColors.surfaceLow,
                  borderRadius: BorderRadius.circular(999),
                  border: Border.all(color: AppColors.border),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Container(
                      width: 8,
                      height: 8,
                      decoration: BoxDecoration(
                        color: AppColors.secondary,
                        shape: BoxShape.circle,
                        boxShadow: [
                          BoxShadow(
                            color: AppColors.secondary.withValues(alpha: .8),
                            blurRadius: 8,
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      AppLocale.tr('home.velocity'),
                      style: const TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.w700,
                        color: AppColors.secondary,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 14),
              Text(
                AppLocale.t('السرعة القصوى تجتمع مع ', 'Maximum velocity meets '),
                style: Theme.of(context).textTheme.headlineMedium,
              ),
              Text(
                AppLocale.t('الفخامة الرقمية', 'digital luxury'),
                style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                      color: AppColors.primary,
                    ),
              ),
              const SizedBox(height: 8),
              Text(
                AppLocale.tr('home.body'),
                style: const TextStyle(
                    fontSize: 13, color: AppColors.textMuted, height: 1.5),
              ),
              const SizedBox(height: 14),
              // Search + go
              _buildSearchBar(context),
              const SizedBox(height: 12),
              Row(
                children: [
                  _TelemetryBadge(
                      icon: Icons.my_location,
                      label: AppLocale.tr('home.liveTracking')),
                  const SizedBox(width: 14),
                  _TelemetryBadge(
                      icon: Icons.verified_user_outlined,
                      label: AppLocale.tr('home.guarantee')),
                  const SizedBox(width: 14),
                  _TelemetryBadge(
                      icon: Icons.timer_outlined,
                      label: AppLocale.tr('home.avgTime')),
                ],
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildSearchBar(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(6),
      decoration: BoxDecoration(
        color: AppColors.surfaceLow,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.border),
      ),
      child: Row(
        children: [
          Expanded(
            child: TextField(
              controller: _searchCtrl,
              style: const TextStyle(color: AppColors.textHigh, fontSize: 14),
              textInputAction: TextInputAction.search,
              onSubmitted: (q) => _goSearch(q),
              decoration: const InputDecoration(
                hintText: '',
                prefixIcon: Icon(Icons.search, color: AppColors.textDisabled),
                border: InputBorder.none,
                enabledBorder: InputBorder.none,
                focusedBorder: InputBorder.none,
                filled: false,
              ),
            ),
          ),
          const SizedBox(width: 6),
          Material(
            color: Colors.transparent,
            child: Ink(
              decoration: AppDecor.primaryGlow(radius: AppDecor.radiusSm),
              child: InkWell(
                borderRadius: BorderRadius.circular(AppDecor.radiusSm),
                onTap: () => _goSearch(_searchCtrl.text),
                child: Padding(
                  padding: const EdgeInsets.symmetric(
                      horizontal: 16, vertical: 11),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        AppLocale.tr('app.go'),
                        style: const TextStyle(
                            color: AppColors.onPrimary,
                            fontWeight: FontWeight.w700,
                            fontSize: 13),
                      ),
                      const SizedBox(width: 6),
                      const Icon(Icons.bolt, color: Colors.white, size: 16),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _goSearch(String q) {
    final query = q.trim();
    if (query.isEmpty) {
      Navigator.of(context).push(
        MaterialPageRoute(builder: (_) => const BrowseScreen(initial: 'all')),
      );
      return;
    }
    Navigator.of(context).push(
      MaterialPageRoute(
          builder: (_) => BrowseScreen(initial: 'all', searchQuery: query)),
    );
  }

  Widget _buildPromoCard(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 20, 16, 4),
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: AppDecor.promoBanner(),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 12, vertical: 5),
                  decoration: BoxDecoration(
                    color: AppColors.tertiary,
                    borderRadius: BorderRadius.circular(999),
                  ),
                  child: Text(
                    AppLocale.tr('home.membersDeal'),
                    style: const TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.w700,
                        color: Colors.white),
                  ),
                ),
                const Spacer(),
                Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    color: AppColors.surfaceLow,
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(Icons.percent,
                      color: AppColors.primary, size: 20),
                ),
              ],
            ),
            const SizedBox(height: 14),
            Text.rich(
              TextSpan(
                text: AppLocale.t('خصم ', 'Save '),
                style: Theme.of(context).textTheme.headlineSmall,
                children: [
                  TextSpan(
                    text: AppLocale.t('30%', '30%'),
                    style: const TextStyle(
                        color: AppColors.primary,
                        fontWeight: FontWeight.w800),
                  ),
                  TextSpan(
                    text: AppLocale.t(' على طلبك الأول', ' on your first order'),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 8),
            Text(
              AppLocale.tr('home.promoDesc'),
              style: const TextStyle(
                  fontSize: 12, color: AppColors.textMuted, height: 1.5),
            ),
            const SizedBox(height: 14),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
              decoration: BoxDecoration(
                color: AppColors.canvas,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppColors.border),
              ),
              child: Row(
                children: [
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        AppLocale.tr('home.promoCode'),
                        style: const TextStyle(
                            fontSize: 10, color: AppColors.textMuted),
                      ),
                      const Text(
                        'VELOX30',
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.w800,
                          letterSpacing: 3,
                          color: AppColors.secondary,
                        ),
                      ),
                    ],
                  ),
                  const Spacer(),
                  TextButton.icon(
                    onPressed: _copyPromo,
                    style: TextButton.styleFrom(
                      backgroundColor: AppColors.surfaceHigh,
                      foregroundColor: AppColors.textHigh,
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(9)),
                    ),
                    icon: const Icon(Icons.copy, size: 15),
                    label: Text(AppLocale.tr('home.copyCode')),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPillars(BuildContext context) {
    final pillars = [
      (
        icon: Icons.restaurant,
        title: AppLocale.tr('home.pillarFood'),
        desc: AppLocale.tr('home.pillarFoodDesc'),
        color: AppColors.primary,
        category: 'food',
      ),
      (
        icon: Icons.checkroom,
        title: AppLocale.tr('home.pillarFashion'),
        desc: AppLocale.tr('home.pillarFashionDesc'),
        color: AppColors.secondary,
        category: 'fashion',
      ),
      (
        icon: Icons.devices_other,
        title: AppLocale.tr('home.pillarTech'),
        desc: AppLocale.tr('home.pillarTechDesc'),
        color: AppColors.tertiary,
        category: 'electronics',
      ),
    ];
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 24, 16, 4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SectionHeader(
            label: AppLocale.tr('home.pillars').toUpperCase(),
            title: AppLocale.tr('home.pillarsTitle'),
          ),
          const SizedBox(height: 12),
          ...pillars.map((pl) => Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: GlassCard(
                  padding: const EdgeInsets.all(16),
                  color: AppColors.surfaceLow,
                  onTap: () => Navigator.of(context).push(MaterialPageRoute(
                      builder: (_) =>
                          BrowseScreen(initial: pl.category))),
                  child: Row(
                    children: [
                      Container(
                        width: 48,
                        height: 48,
                        decoration: BoxDecoration(
                          color: AppColors.surfaceHigh,
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(
                              color: pl.color.withValues(alpha: .5)),
                        ),
                        child:
                            Icon(pl.icon, color: pl.color, size: 24),
                      ),
                      const SizedBox(width: 14),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(pl.title,
                                style: Theme.of(context)
                                    .textTheme
                                    .titleMedium),
                            const SizedBox(height: 4),
                            Text(pl.desc,
                                style: const TextStyle(
                                    fontSize: 12,
                                    color: AppColors.textMuted,
                                    height: 1.4)),
                          ],
                        ),
                      ),
                      const Icon(Icons.chevron_left,
                          color: AppColors.textDisabled),
                    ],
                  ),
                ),
              )),
        ],
      ),
    );
  }
}

class _LocationChip extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: AppColors.surfaceLow,
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: AppColors.border),
      ),
      child: const Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.near_me, size: 14, color: AppColors.primary),
          SizedBox(width: 4),
          Text(
            'DAMIETTA',
            style: TextStyle(
                fontSize: 10, fontWeight: FontWeight.w700, color: AppColors.textHigh),
          ),
          Icon(Icons.expand_more, size: 14, color: AppColors.textMuted),
        ],
      ),
    );
  }
}

class _CartIconButton extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Consumer<CartState>(
      builder: (_, cart, __) => IconButton(
        onPressed: () => Navigator.of(context).push(
          MaterialPageRoute(builder: (_) => const CartScreen()),
        ),
        icon: Badge(
          isLabelVisible: cart.count > 0,
          label: Text('${cart.count}'),
          backgroundColor: AppColors.primary,
          child: const Icon(Icons.shopping_bag_outlined),
        ),
      ),
    );
  }
}

class _TelemetryBadge extends StatelessWidget {
  final IconData icon;
  final String label;

  const _TelemetryBadge({required this.icon, required this.label});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 15, color: AppColors.secondary),
        const SizedBox(width: 5),
        Flexible(
          child: Text(
            label,
            style: const TextStyle(fontSize: 10, color: AppColors.textMuted),
          ),
        ),
      ],
    );
  }
}