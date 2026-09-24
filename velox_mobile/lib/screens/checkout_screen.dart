import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/api_client.dart';
import '../core/app_config.dart';
import '../core/locale.dart';
import '../models/order_summary.dart';
import '../services/auth_service.dart';
import '../services/cart_state.dart';
import '../services/checkout_logic.dart';
import '../services/location_service.dart';
import '../theme/app_theme.dart';
import '../widgets/glass_card.dart';
import '../widgets/primary_button.dart';
import 'auth/login_screen.dart';
import 'main_shell.dart';

class CheckoutScreen extends StatefulWidget {
  const CheckoutScreen({super.key});

  @override
  State<CheckoutScreen> createState() => _CheckoutScreenState();
}

class _CheckoutScreenState extends State<CheckoutScreen> {
  int _step = 0; // 0 address, 1 payment, 2 confirm

  final _nameCtrl = TextEditingController();
  final _phoneCtrl = TextEditingController();
  final _addressCtrl = TextEditingController();
  final _cardNumberCtrl = TextEditingController();
  final _cardHolderCtrl = TextEditingController();
  final _cardExpiryCtrl = TextEditingController();
  final _cardCvvCtrl = TextEditingController();

  String _gov = 'CAIRO';
  String _payment = 'CASH_ON_DELIVERY';
  bool _govAutoDetected = false;
  bool _locating = false;
  bool _placing = false;

  bool _anon = false;

  @override
  void initState() {
    super.initState();
    // (أ) name + phone auto-filled from the registered account (editable).
    final prof = AuthService.instance.profile;
    if (prof == null) {
      _anon = true;
    } else {
      _nameCtrl.text = prof.fullName;
      _phoneCtrl.text = prof.phone;
      _gov = prof.governorate.isNotEmpty ? prof.governorate : _gov;
    }
    // (ب) auto-detect the governorate from device location.
    _detectLocation();
  }

  @override
  void dispose() {
    _nameCtrl.dispose();
    _phoneCtrl.dispose();
    _addressCtrl.dispose();
    _cardNumberCtrl.dispose();
    _cardHolderCtrl.dispose();
    _cardExpiryCtrl.dispose();
    _cardCvvCtrl.dispose();
    super.dispose();
  }

  Future<void> _detectLocation() async {
    setState(() => _locating = true);
    final gov = await LocationService.instance.detectGovernorate();
    if (!mounted) return;
    setState(() {
      _locating = false;
      if (gov != null) {
        _gov = gov;
        _govAutoDetected = true;
      }
    });
  }

  bool _validateAddress() {
    final nameOk = _nameCtrl.text.trim().length >= 2;
    final phoneOk =
        RegExp(r'^01\d{9}$').hasMatch(_phoneCtrl.text.replaceAll(' ', ''));
    if (!nameOk || !phoneOk) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Text(AppLocale.t(
            'أكمل بيانات الاسم ورقم الهاتف بشكل صحيح.',
            'Complete name and phone correctly.')),
      ));
      return false;
    }
    return true;
  }

  bool _validatePayment() {
    if (_payment == 'CASH_ON_DELIVERY' || _payment == 'WALLET') return true;
    final numOk = RegExp(r'^\d{16}$')
        .hasMatch(_cardNumberCtrl.text.replaceAll(' ', ''));
    final expOk = RegExp(r'^(0[1-9]|1[0-2])/\d{2}$')
        .hasMatch(_cardExpiryCtrl.text.trim());
    final cvvOk =
        RegExp(r'^\d{3,4}$').hasMatch(_cardCvvCtrl.text.trim());
    final holderOk = _cardHolderCtrl.text.trim().length >= 3;
    if (!numOk || !expOk || !cvvOk || !holderOk) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Text(AppLocale.t(
            'أكمل بيانات طريقة الدفع بشكل صحيح.',
            'Complete the payment details correctly.')),
      ));
      return false;
    }
    return true;
  }

  Future<void> _placeOrder() async {
    final cart = CartState.instance;
    if (cart.isEmpty) return;
    setState(() => _placing = true);
    try {
      final items = cart.items
          .map((i) => {
                'product_id': i.product.id,
                'quantity': i.qty,
              })
          .toList();
      final address =
          CheckoutData.buildShippingAddress(
        govId: _gov,
        address: _addressCtrl.text,
        phone: _phoneCtrl.text,
      );

      if (useMockApi) {
        // demo path: no backend call, synthesize a clean summary
        await Future.delayed(const Duration(milliseconds: 600));
        final total = cart.subtotal +
            CheckoutData.byId(_gov).shippingFee;
        final ok = OrderSummary(
          orderCode:
              'VELOX-DEMO-${DateTime.now().millisecondsSinceEpoch % 100000}',
          orderId: 0,
          status: 'PENDING',
          total: total,
          paymentMethod: _payment,
        );
        cart.clear();
        if (!mounted) return;
        setState(() => _placing = false);
        _showSuccess(ok);
        return;
      }

      final data = await ApiClient.instance.request(
        AppEndpoints.orders,
        method: 'POST',
        auth: true,
        body: {
          'items': items,
          'governorate': _gov,
          'paymentMethod': _payment,
          'address': address,
          'shipping_address': address,
        },
      );
      final summary = OrderSummary.fromJson(
          Map<String, dynamic>.from(data as Map<String, dynamic>));
      cart.clear();
      if (!mounted) return;
      setState(() => _placing = false);
      _showSuccess(summary);
    } catch (e) {
      if (!mounted) return;
      setState(() => _placing = false);
      final msg = e is ApiException ? e.message : '$e';
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(AppLocale.t(msg, msg))),
      );
    }
  }

  void _showSuccess(OrderSummary order) {
    Navigator.of(context).pushReplacement(MaterialPageRoute(
      builder: (_) => _OrderSuccessScreen(order: order),
    ));
  }

  @override
  Widget build(BuildContext context) {
    final cart = context.watch<CartState>();
    final steps = [
      AppLocale.tr('co.stepAddress'),
      AppLocale.tr('co.stepPayment'),
      AppLocale.tr('co.stepConfirm'),
    ];
    final foodOnly = cart.isFoodOnly;
    final eta = CheckoutData.etaText(_gov, foodOnly: foodOnly);

    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocale.tr('co.title')),
        leading: _step > 0
            ? IconButton(
                icon: const Icon(Icons.arrow_back),
                onPressed: () => setState(() => _step -= 1),
              )
            : null,
      ),
      body: Column(
        children: [
          // step indicator
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 4),
            child: Row(
              children: List.generate(steps.length, (i) {
                final active = i == _step;
                final done = i < _step;
                return Expanded(
                  child: Row(
                    children: [
                      Expanded(
                        child: Column(
                          children: [
                            Container(
                              width: 28,
                              height: 28,
                              alignment: Alignment.center,
                              decoration: BoxDecoration(
                                shape: BoxShape.circle,
                                color: done
                                    ? AppColors.secondary
                                    : active
                                        ? AppColors.primary
                                        : AppColors.surfaceHigh,
                                border: Border.all(
                                    color: active
                                        ? AppColors.primary
                                        : AppColors.border),
                              ),
                              child: done
                                  ? const Icon(Icons.check,
                                      size: 15, color: Color(0xFF00363D))
                                  : Text('${i + 1}',
                                      style: const TextStyle(
                                          fontSize: 12,
                                          fontWeight: FontWeight.w700,
                                          color: AppColors.textHigh)),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              steps[i],
                              style: TextStyle(
                                fontSize: 10,
                                fontWeight: FontWeight.w700,
                                color: active
                                    ? AppColors.textHigh
                                    : AppColors.textMuted,
                              ),
                            ),
                          ],
                        ),
                      ),
                      if (i < steps.length - 1)
                        Container(
                          height: 1,
                          margin: const EdgeInsets.only(bottom: 16),
                          color: done
                              ? AppColors.secondary
                              : AppColors.border,
                        ),
                    ],
                  ),
                );
              }),
            ),
          ),
          const SizedBox(height: 6),
          Expanded(
            child: switch (_step) {
              0 => _buildAddressStep(eta),
              1 => _buildPaymentStep(),
              _ => _buildConfirmStep(cart, eta),
            },
          ),
        ],
      ),
    );
  }

  // ---------------- Step 1: Address ----------------

  Widget _buildAddressStep(String eta) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        GlassCard(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              TextField(
                controller: _nameCtrl,
                readOnly: _anon,
                decoration: InputDecoration(
                  labelText: AppLocale.tr('co.name'),
                  helperText: AppLocale.tr('co.nameAuto'),
                  prefixIcon: const Icon(Icons.person_outline,
                      color: AppColors.textMuted),
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _phoneCtrl,
                readOnly: _anon,
                keyboardType: TextInputType.phone,
                decoration: InputDecoration(
                  labelText: AppLocale.tr('co.phone'),
                  prefixIcon: const Icon(Icons.phone_outlined,
                      color: AppColors.textMuted),
                ),
              ),
              const SizedBox(height: 12),
              // Governorate select + location auto-detect
              Row(
                children: [
                  Expanded(
                    child: DropdownButtonFormField<String>(
                      key: ValueKey('gov-$_gov'),
                      initialValue: _gov,
                      decoration: InputDecoration(
                        labelText: AppLocale.tr('co.governorate'),
                        prefixIcon: const Icon(Icons.map_outlined,
                            color: AppColors.textMuted),
                      ),
                      items: CheckoutData.all
                          .map((g) => DropdownMenuItem(
                                value: g.id,
                                child: Text(
                                  '${g.nameAr} — ${g.nameEn}',
                                  style: const TextStyle(fontSize: 13),
                                ),
                              ))
                          .toList(),
                      onChanged: (v) {
                        if (v != null) setState(() => _gov = v);
                      },
                    ),
                  ),
                  const SizedBox(width: 8),
                  InkWell(
                    onTap: _locating ? null : _detectLocation,
                    borderRadius: BorderRadius.circular(10),
                    child: Container(
                      width: 48,
                      height: 48,
                      decoration: BoxDecoration(
                        color: _govAutoDetected
                            ? AppColors.secondary.withValues(alpha: .15)
                            : AppColors.surfaceHigh,
                        borderRadius: BorderRadius.circular(10),
                        border: Border.all(
                            color: _govAutoDetected
                                ? AppColors.secondary
                                : AppColors.border),
                      ),
                      child: _locating
                          ? const Padding(
                              padding: EdgeInsets.all(13),
                              child:
                                  CircularProgressIndicator(strokeWidth: 2))
                          : Icon(
                              _govAutoDetected
                                  ? Icons.my_location
                                  : Icons.my_location_outlined,
                              color: _govAutoDetected
                                  ? AppColors.secondary
                                  : AppColors.textMuted),
                    ),
                  ),
                ],
              ),
              if (_govAutoDetected) ...[
                const SizedBox(height: 6),
                Row(
                  children: [
                    const Icon(Icons.check_circle,
                        size: 13, color: AppColors.secondary),
                    const SizedBox(width: 5),
                    Text(
                      '${AppLocale.tr('co.governorateDetected')}: ${CheckoutData.byId(_gov).nameAr}',
                      style: const TextStyle(
                          fontSize: 11, color: AppColors.secondary),
                    ),
                  ],
                ),
              ],
              const SizedBox(height: 12),
              TextField(
                controller: _addressCtrl,
                maxLines: 2,
                decoration: InputDecoration(
                  labelText: AppLocale.tr('co.address'),
                  prefixIcon: const Icon(Icons.location_on_outlined,
                      color: AppColors.textMuted),
                ),
              ),
              const SizedBox(height: 14),
              // ETA chip — food: minutes | fashion/electronics: days
              Row(
                children: [
                  const Icon(Icons.bolt,
                      size: 15, color: AppColors.primary),
                  const SizedBox(width: 6),
                  Text(
                    eta,
                    style: const TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w700,
                        color: AppColors.textHigh),
                  ),
                  const Spacer(),
                  _paymentMethodPreview(),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 20),
        PrimaryButton(
          label: '${AppLocale.tr('co.stepPayment')} ${AppLocale.isAr ? '←' : '→'}',
          onPressed: () {
            if (_validateAddress()) setState(() => _step = 1);
          },
        ),
        if (_anon) ...[
          const SizedBox(height: 10),
          TextButton(
            onPressed: () => Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => const LoginScreen())),
            child: Text(
              AppLocale.tr('co.loginRequired'),
              style: const TextStyle(color: AppColors.secondary),
            ),
          ),
        ],
      ],
    );
  }

  Widget _paymentMethodPreview() {
    final label = switch (_payment) {
      'WALLET' => AppLocale.tr('co.wallet'),
      'VISA' => AppLocale.tr('co.visa'),
      _ => AppLocale.tr('co.cod'),
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: AppColors.surfaceHigh,
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: AppColors.border),
      ),
      child: Text(label,
          style: const TextStyle(fontSize: 11, color: AppColors.textMuted)),
    );
  }

  // ---------------- Step 2: Payment ----------------

  Widget _buildPaymentStep() {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _PayTile(
          icon: Icons.payments_outlined,
          title: AppLocale.tr('co.cod'),
          selected: _payment == 'CASH_ON_DELIVERY',
          onTap: () => setState(() => _payment = 'CASH_ON_DELIVERY'),
        ),
        const SizedBox(height: 10),
        _PayTile(
          icon: Icons.account_balance_wallet_outlined,
          title: AppLocale.tr('co.wallet'),
          trailing: const Text('0.00 EGP',
              style: TextStyle(fontSize: 12, color: AppColors.textMuted)),
          selected: _payment == 'WALLET',
          onTap: () => setState(() => _payment = 'WALLET'),
        ),
        const SizedBox(height: 10),
        _PayTile(
          icon: Icons.credit_card,
          title: AppLocale.tr('co.visa'),
          selected: _payment == 'VISA',
          onTap: () => setState(() => _payment = 'VISA'),
        ),
        if (_payment == 'VISA') ...[
          const SizedBox(height: 16),
          GlassCard(
            padding: const EdgeInsets.all(16),
            child: Column(
              children: [
                TextField(
                  controller: _cardNumberCtrl,
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(
                    labelText: '•••• •••• •••• ••••',
                    prefixIcon:
                        Icon(Icons.credit_card, color: AppColors.textMuted),
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: _cardHolderCtrl,
                  decoration: InputDecoration(
                    labelText: AppLocale.tr('co.cardHolder'),
                    prefixIcon: const Icon(Icons.person_outline,
                        color: AppColors.textMuted),
                  ),
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _cardExpiryCtrl,
                        keyboardType: TextInputType.datetime,
                        decoration: InputDecoration(
                          labelText: '${AppLocale.tr('co.expiry')} (MM/YY)',
                          prefixIcon: const Icon(Icons.calendar_today,
                              size: 16, color: AppColors.textMuted),
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: TextField(
                        controller: _cardCvvCtrl,
                        keyboardType: TextInputType.number,
                        obscureText: true,
                        decoration: const InputDecoration(
                          labelText: 'CVV',
                          prefixIcon: Icon(Icons.lock_outline,
                              size: 16, color: AppColors.textMuted),
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
        const SizedBox(height: 24),
        PrimaryButton(
          label: '${AppLocale.tr('co.stepConfirm')} ${AppLocale.isAr ? '←' : '→'}',
          onPressed: () {
            if (_validatePayment()) setState(() => _step = 2);
          },
        ),
      ],
    );
  }

  // ---------------- Step 3: Confirm ----------------

  Widget _buildConfirmStep(CartState cart, String eta) {
    final subtotal = cart.subtotal;
    final shipping = CheckoutData.byId(_gov).shippingFee.toDouble();
    final total = subtotal + shipping;

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        GlassCard(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(AppLocale.tr('co.stepAddress'),
                  style: const TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w800,
                      letterSpacing: 1.2,
                      color: AppColors.primary)),
              const SizedBox(height: 8),
              Text(_nameCtrl.text.trim(),
                  style: const TextStyle(
                      fontSize: 16, fontWeight: FontWeight.w800)),
              const SizedBox(height: 4),
              Text(
                '${CheckoutData.byId(_gov).nameAr} · ${_phoneCtrl.text.trim()}',
                style: const TextStyle(
                    fontSize: 12, color: AppColors.textMuted),
              ),
              if (_addressCtrl.text.trim().isNotEmpty) ...[
                const SizedBox(height: 4),
                Text(_addressCtrl.text.trim(),
                    style: const TextStyle(
                        fontSize: 12, color: AppColors.textMuted)),
              ],
              const SizedBox(height: 10),
              Row(
                children: [
                  const Icon(Icons.bolt, size: 14, color: AppColors.primary),
                  const SizedBox(width: 5),
                  Text(eta,
                      style: const TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w700,
                          color: AppColors.textHigh)),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        GlassCard(
          padding: const EdgeInsets.all(16),
          child: Column(
            children: [
              ...cart.items.map((i) => Padding(
                    padding: const EdgeInsets.symmetric(vertical: 4),
                    child: Row(
                      children: [
                        Expanded(
                          child: Text(
                            '${i.product.nameFor(AppLocale.lang)} × ${i.qty}',
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(fontSize: 13),
                          ),
                        ),
                        Text(AppLocale.money(i.lineTotal),
                            style: const TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.w700)),
                      ],
                    ),
                  )),
              const Divider(height: 18),
              _row(AppLocale.tr('cart.subtotal'), AppLocale.money(subtotal)),
              _row(AppLocale.tr('cart.shipping'), AppLocale.money(shipping)),
              const SizedBox(height: 4),
              _row(AppLocale.tr('cart.total'), AppLocale.money(total),
                  bold: true, color: AppColors.primary),
            ],
          ),
        ),
        const SizedBox(height: 20),
        PrimaryButton(
          label: AppLocale.tr('co.place'),
          icon: Icons.bolt,
          loading: _placing,
          onPressed: _placing ? null : _placeOrder,
        ),
      ],
    );
  }

  Widget _row(String label, String value,
      {bool bold = false, Color color = AppColors.textHigh}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        children: [
          Text(label,
              style: TextStyle(
                  fontSize: bold ? 15 : 13,
                  fontWeight: bold ? FontWeight.w800 : FontWeight.w500,
                  color: bold ? color : AppColors.textMuted)),
          const Spacer(),
          Text(value,
              style: TextStyle(
                  fontSize: bold ? 16 : 13,
                  fontWeight: bold ? FontWeight.w800 : FontWeight.w600,
                  color: color)),
        ],
      ),
    );
  }
}

class _PayTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final bool selected;
  final VoidCallback onTap;
  final Widget? trailing;

  const _PayTile({
    required this.icon,
    required this.title,
    required this.selected,
    required this.onTap,
    this.trailing,
  });

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
      color: selected
          ? AppColors.secondary.withValues(alpha: .08)
          : AppColors.surfaceLow,
      borderColor: selected ? AppColors.secondary : null,
      onTap: onTap,
      child: Row(
        children: [
          Icon(icon,
              color: selected ? AppColors.secondary : AppColors.textMuted),
          const SizedBox(width: 12),
          Expanded(
              child: Text(title,
                  style: const TextStyle(
                      fontSize: 14, fontWeight: FontWeight.w600))),
          if (trailing != null) ...[trailing!, const SizedBox(width: 8)],
          Icon(
            selected
                ? Icons.radio_button_checked
                : Icons.radio_button_off,
            size: 20,
            color: selected ? AppColors.secondary : AppColors.textDisabled,
          ),
        ],
      ),
    );
  }
}

/// Post-order success screen (order code + status).
class _OrderSuccessScreen extends StatelessWidget {
  final OrderSummary order;

  const _OrderSuccessScreen({required this.order});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Container(
                width: 96,
                height: 96,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: AppColors.success.withValues(alpha: .12),
                  border: Border.all(color: AppColors.success),
                ),
                child: const Icon(Icons.check,
                    size: 48, color: AppColors.success),
              ),
              const SizedBox(height: 24),
              Text(AppLocale.tr('co.success'),
                  style: Theme.of(context).textTheme.headlineMedium,
                  textAlign: TextAlign.center),
              const SizedBox(height: 16),
              GlassCard(
                padding: const EdgeInsets.all(20),
                child: Column(
                  children: [
                    Text(AppLocale.tr('co.orderCode'),
                        style: const TextStyle(
                            fontSize: 12, color: AppColors.textMuted)),
                    const SizedBox(height: 6),
                    Text(
                      order.orderCode,
                      style: const TextStyle(
                        fontSize: 22,
                        fontWeight: FontWeight.w800,
                        letterSpacing: 2,
                        color: AppColors.secondary,
                      ),
                    ),
                    const SizedBox(height: 12),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 10, vertical: 4),
                          decoration: BoxDecoration(
                            color:
                                AppColors.primary.withValues(alpha: .15),
                            borderRadius: BorderRadius.circular(999),
                          ),
                          child: Text(
                            order.statusLabelAr,
                            style: const TextStyle(
                                fontSize: 11,
                                fontWeight: FontWeight.w700,
                                color: AppColors.primary),
                          ),
                        ),
                        const SizedBox(width: 10),
                        Text(AppLocale.money(order.total),
                            style: const TextStyle(
                                fontWeight: FontWeight.w800,
                                fontSize: 15)),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 28),
              PrimaryButton(
                label: AppLocale.tr('co.continueShopping'),
                icon: Icons.arrow_back,
                variant: 'secondary',
                onPressed: () => Navigator.of(context).pushAndRemoveUntil(
                  MaterialPageRoute(builder: (_) => const MainShell()),
                  (route) => false,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}