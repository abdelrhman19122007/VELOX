import 'package:flutter/material.dart';

import '../core/api_client.dart';
import '../core/app_config.dart';
import '../core/locale.dart';
import '../models/order_summary.dart';
import '../services/auth_service.dart';
import '../theme/app_theme.dart';
import '../widgets/empty_state.dart';
import '../widgets/glass_card.dart';
import 'auth/login_screen.dart';

class OrdersScreen extends StatefulWidget {
  const OrdersScreen({super.key});

  @override
  State<OrdersScreen> createState() => _OrdersScreenState();
}

class _OrdersScreenState extends State<OrdersScreen> {
  List<OrderSummary>? _orders;
  bool _loading = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    if (!AuthService.instance.isLoggedIn) return;
    setState(() => _loading = true);
    try {
      final data = await ApiClient.instance.request(
        AppEndpoints.orderHistory,
        auth: true,
      );
      final list = data is List ? data : (data?['orders'] as List? ?? []);
      final orders = list
          .map((e) => OrderSummary.fromJson(
              Map<String, dynamic>.from(e as Map<String, dynamic>)))
          .toList();
      if (!mounted) return;
      setState(() {
        _orders = orders;
        _error = null;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = e is ApiException ? e.message : '$e';
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(AppLocale.tr('ord.title'))),
      body: Builder(builder: (context) {
        if (!AuthService.instance.isLoggedIn) {
          return EmptyState(
            icon: Icons.receipt_long_outlined,
            title: AppLocale.tr('acct.notLoggedIn'),
            hint: AppLocale.tr('acct.loginPrompt'),
            actionLabel: AppLocale.tr('auth.login'),
            onAction: () => Navigator.of(context).push(
              MaterialPageRoute(builder: (_) => const LoginScreen()),
            ),
          );
        }
        if (_loading) {
          return const Center(child: CircularProgressIndicator());
        }
        if (_error != null) {
          return Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.cloud_off,
                    size: 40, color: AppColors.textDisabled),
                const SizedBox(height: 12),
                Text(_error!,
                    style: const TextStyle(
                        fontSize: 13, color: AppColors.textMuted),
                    textAlign: TextAlign.center),
                const SizedBox(height: 10),
                TextButton.icon(
                  onPressed: _load,
                  icon: const Icon(Icons.refresh, size: 16),
                  label: Text(AppLocale.tr('misc.retry')),
                ),
              ],
            ),
          );
        }
        if (_orders == null || _orders!.isEmpty) {
          return EmptyState(
            icon: Icons.receipt_long_outlined,
            title: AppLocale.tr('ord.empty'),
            hint: AppLocale.tr('ord.emptyHint'),
          );
        }
        return RefreshIndicator(
          onRefresh: _load,
          color: AppColors.primary,
          backgroundColor: AppColors.surfaceHigh,
          child: ListView.separated(
            padding: const EdgeInsets.all(16),
            itemCount: _orders!.length,
            separatorBuilder: (_, __) => const SizedBox(height: 10),
            itemBuilder: (_, i) => _OrderCard(order: _orders![i]),
          ),
        );
      }),
    );
  }
}

class _OrderCard extends StatelessWidget {
  final OrderSummary order;

  const _OrderCard({required this.order});

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  '${AppLocale.tr('ord.order')} #${order.orderCode}',
                  style: const TextStyle(
                      fontSize: 14, fontWeight: FontWeight.w700),
                ),
              ),
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: AppColors.primary.withValues(alpha: .14),
                  borderRadius: BorderRadius.circular(999),
                  border:
                      Border.all(color: AppColors.primary.withValues(alpha: .5)),
                ),
                child: Text(
                  order.statusLabelAr,
                  style: const TextStyle(
                      fontSize: 10,
                      fontWeight: FontWeight.w700,
                      color: AppColors.primary),
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              const Icon(Icons.bolt, size: 14, color: AppColors.primary),
              const SizedBox(width: 5),
              Text(
                '${AppLocale.tr('co.etaFood')} ${CheckoutEta.placeholder()}',
                style: const TextStyle(
                    fontSize: 11, color: AppColors.textMuted),
              ),
              const Spacer(),
              Text(
                AppLocale.money(order.total),
                style: const TextStyle(
                    fontSize: 15, fontWeight: FontWeight.w800),
              ),
            ],
          ),
          if (order.createdAt != null && order.createdAt!.isNotEmpty) ...[
            const SizedBox(height: 6),
            Text(
              order.createdAt!,
              style: const TextStyle(
                  fontSize: 10, color: AppColors.textDisabled),
            ),
          ],
        ],
      ),
    );
  }
}

/// Small ETA placeholder so orders cards remain informative.
class CheckoutEta {
  static String placeholder() =>
      AppLocale.t('بالدقائق حسب موقعك', 'in minutes based on location');
}