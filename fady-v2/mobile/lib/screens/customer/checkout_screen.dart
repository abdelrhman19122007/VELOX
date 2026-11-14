import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/app_state.dart';
import '../../core/models.dart';
import '../../widgets/common.dart';
import 'payment_screen.dart';
import 'track_screen.dart';

/// Checkout: address with landmark (or a saved one), when, how to pay, and the final price.
/// Also used for "bring me anything" errands ([errandDetails] set): no items, the driver buys them.
class CheckoutScreen extends StatefulWidget {
  final Quote quote;
  final String? errandDetails;
  const CheckoutScreen({super.key, required this.quote, this.errandDetails});
  @override
  State<CheckoutScreen> createState() => _CheckoutScreenState();
}

class _CheckoutScreenState extends State<CheckoutScreen> {
  final _form = GlobalKey<FormState>();
  late final TextEditingController phone, address, landmark;
  final change = TextEditingController(), walletPhone = TextEditingController();
  String method = 'CASH';
  DateTime? scheduled;
  bool saveIt = false, placing = false;
  double? walletBalance;
  late Quote quote;

  bool get errand => widget.errandDetails != null;

  @override
  void initState() {
    super.initState();
    final s = context.read<AppState>();
    quote = widget.quote;
    phone = TextEditingController(text: s.phone.isEmpty ? (s.user?.phone ?? '') : s.phone);
    address = TextEditingController(text: s.address);
    landmark = TextEditingController(text: s.landmark);
    walletPhone.text = phone.text.startsWith('010') ? phone.text : '';
    s.wallet().then((w) {
      if (mounted) setState(() => walletBalance = w.balance);
    }).catchError((_) {});
  }

  Future<void> _requote(AppState s) async {
    try {
      final q = await s.quote(errand: errand);
      if (mounted) setState(() => quote = q);
    } catch (_) {}
  }

  Future<void> _pickTime() async {
    final now = DateTime.now();
    final day = await showDatePicker(context: context, firstDate: now, lastDate: now.add(const Duration(days: 7)), initialDate: now);
    if (day == null || !mounted) return;
    final time = await showTimePicker(context: context, initialTime: TimeOfDay.fromDateTime(now.add(const Duration(hours: 1))));
    if (time == null) return;
    setState(() => scheduled = DateTime(day.year, day.month, day.day, time.hour, time.minute));
  }

  Future<void> _place() async {
    if (!_form.currentState!.validate()) return;
    final s = context.read<AppState>();
    s.phone = phone.text.trim();
    s.address = address.text.trim();
    s.landmark = landmark.text.trim();
    setState(() => placing = true);
    final order = await busy(context, () async {
      if (saveIt) await s.saveAddress(s.ar ? 'البيت' : 'Home');
      return s.placeOrder(
        method: method,
        walletPhone: walletPhone.text,
        changeFor: method == 'CASH' ? double.tryParse(change.text.trim()) : null,
        scheduledFor: scheduled,
        errandDetails: widget.errandDetails,
      );
    });
    if (!mounted) return;
    setState(() => placing = false);
    if (order == null) return;
    Navigator.of(context).popUntil((r) => r.isFirst);
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => order.waitingPayment ? PaymentScreen(code: order.code) : TrackScreen(code: order.code)),
    );
  }

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    final methods = errand ? ['CASH'] : ['CASH', 'VODAFONE_CASH', 'FAWRY', 'WALLET'];
    return Scaffold(
      appBar: AppBar(title: Text(errand ? s.t('errand') : s.t('checkout'))),
      body: Form(
        key: _form,
        child: ListView(padding: const EdgeInsets.all(16), children: [
          if (s.addresses.isNotEmpty) ...[
            Text(s.t('savedAddresses'), style: tt.titleMedium?.copyWith(fontWeight: FontWeight.w800)),
            const SizedBox(height: 8),
            Wrap(spacing: 8, runSpacing: 8, children: [
              for (final a in s.addresses)
                ActionChip(
                  avatar: const Icon(Icons.bookmark_outline, size: 18),
                  label: Text(a.label),
                  onPressed: () {
                    s.useAddress(a);
                    phone.text = a.phone;
                    address.text = a.address;
                    landmark.text = a.landmark ?? '';
                    _requote(s);
                  },
                ),
            ]),
            const SizedBox(height: 16),
          ],
          DropdownButtonFormField<String>(
            initialValue: s.zoneOf(s.zone) == null ? null : s.zone,
            decoration: InputDecoration(labelText: s.t('zone'), prefixIcon: const Icon(Icons.map_outlined)),
            items: [
              for (final z in s.zones)
                DropdownMenuItem(value: z.code, child: Text('${s.ar ? z.nameAr : z.nameEn} · ${s.money(z.fee)}')),
            ],
            onChanged: (z) {
              if (z == null) return;
              s.setZone(z);
              _requote(s);
            },
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: address,
            decoration: InputDecoration(labelText: s.t('address'), hintText: s.t('addressHint'), prefixIcon: const Icon(Icons.home_outlined)),
            validator: (v) => (v ?? '').trim().length < 5 ? s.t('addressHint') : (RegExp(r'[<>]').hasMatch(v!) ? s.t('err_VALIDATION') : null),
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: landmark,
            decoration: InputDecoration(labelText: s.t('landmark'), hintText: s.t('landmarkHint'), prefixIcon: const Icon(Icons.flag_outlined)),
            validator: (v) => RegExp(r'[<>]').hasMatch(v ?? '') ? s.t('err_VALIDATION') : null,
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: phone,
            keyboardType: TextInputType.phone,
            decoration: InputDecoration(labelText: s.t('phone'), prefixIcon: const Icon(Icons.phone_outlined)),
            validator: (v) => RegExp(r'^01[0125]\d{8}$').hasMatch((v ?? '').trim()) ? null : s.t('badPhone'),
          ),
          CheckboxListTile(
            contentPadding: EdgeInsets.zero,
            value: saveIt,
            onChanged: (v) => setState(() => saveIt = v ?? false),
            title: Text(s.t('saveAddress')),
          ),
          const SizedBox(height: 4),
          Text(s.t('when'), style: tt.titleMedium?.copyWith(fontWeight: FontWeight.w800)),
          const SizedBox(height: 8),
          SegmentedButton<bool>(
            segments: [
              ButtonSegment(value: false, label: Text(s.t('asap')), icon: const Icon(Icons.flash_on)),
              ButtonSegment(value: true, label: Text(scheduled == null ? s.t('schedule') : timeText(context, scheduled)), icon: const Icon(Icons.event)),
            ],
            selected: {scheduled != null},
            onSelectionChanged: (v) => v.first ? _pickTime() : setState(() => scheduled = null),
          ),
          const SizedBox(height: 20),
          Text(s.t('payWith'), style: tt.titleMedium?.copyWith(fontWeight: FontWeight.w800)),
          const SizedBox(height: 4),
          RadioGroup<String>(
            groupValue: method,
            onChanged: (v) => setState(() => method = v ?? 'CASH'),
            child: Column(children: [
              for (final m in methods)
                RadioListTile<String>(
                  contentPadding: EdgeInsets.zero,
                  value: m,
                  title: Text(s.t('pay_$m')),
                  subtitle: m == 'WALLET' && walletBalance != null ? Text('${s.t('balance')}: ${s.money(walletBalance!)}') : null,
                  secondary: Icon(switch (m) {
                    'CASH' => Icons.payments_outlined,
                    'VODAFONE_CASH' => Icons.phone_iphone,
                    'FAWRY' => Icons.storefront_outlined,
                    _ => Icons.account_balance_wallet_outlined,
                  }),
                ),
            ]),
          ),
          if (method == 'CASH')
            TextFormField(
              controller: change,
              keyboardType: TextInputType.number,
              decoration: InputDecoration(labelText: s.t('changeFor'), hintText: s.t('changeForHint')),
              validator: (v) {
                if ((v ?? '').trim().isEmpty) return null;
                final n = double.tryParse(v!.trim());
                return n == null || n <= quote.total ? s.t('err_INVALID_CHANGE') : null;
              },
            ),
          if (method == 'VODAFONE_CASH')
            TextFormField(
              controller: walletPhone,
              keyboardType: TextInputType.phone,
              decoration: InputDecoration(labelText: s.t('walletPhone'), prefixIcon: const Icon(Icons.phone_iphone)),
              validator: (v) => RegExp(r'^010\d{8}$').hasMatch((v ?? '').trim()) ? null : s.t('err_INVALID_WALLET'),
            ),
          if (errand)
            Padding(
              padding: const EdgeInsets.only(top: 8),
              child: Text(s.t('errandNote'), style: TextStyle(color: cs.onSurfaceVariant)),
            ),
          const SizedBox(height: 16),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(14),
              child: Column(children: [
                if (!errand) PriceRow(s.t('subtotal'), s.money(quote.subtotal)),
                PriceRow(s.t('deliveryFee'), quote.deliveryFee == 0 ? s.t('freeDelivery') : s.money(quote.deliveryFee)),
                if (quote.discount > 0) PriceRow(s.t('discount'), '− ${s.money(quote.discount)}', color: cs.secondary),
                const Divider(height: 18),
                PriceRow(s.t('total'), s.money(quote.total), bold: true),
              ]),
            ),
          ),
          const SizedBox(height: 90),
        ]),
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 4, 16, 10),
          child: FilledButton(
            onPressed: placing ? null : _place,
            child: placing
                ? const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2.5))
                : Text('${s.t('placeOrder')} · ${s.money(quote.total)}'),
          ),
        ),
      ),
    );
  }
}
