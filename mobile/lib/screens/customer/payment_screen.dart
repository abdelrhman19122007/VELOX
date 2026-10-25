import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../../core/app_state.dart';
import '../../core/models.dart';
import '../../widgets/common.dart';
import 'track_screen.dart';

/// Paying an online order: the Vodafone Cash code from the SMS, or the Fawry reference to pay at an outlet.
/// A countdown shows the time left; when the order is paid the store gets it and the app moves to tracking.
class PaymentScreen extends StatefulWidget {
  final String code;
  const PaymentScreen({super.key, required this.code});
  @override
  State<PaymentScreen> createState() => _PaymentScreenState();
}

class _PaymentScreenState extends State<PaymentScreen> {
  PaymentInfo? info;
  Object? error;
  final sms = TextEditingController();
  Timer? _tick;
  bool sending = false;
  late final AppState _s;

  @override
  void initState() {
    super.initState();
    _s = context.read<AppState>();
    _load();
    _tick = Timer.periodic(const Duration(seconds: 1), (_) => setState(() {}));
    _s.watchOrder(widget.code, _onOrder);
  }

  @override
  void dispose() {
    _tick?.cancel();
    _s.unwatchOrder(widget.code, _onOrder);
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final p = await _s.payment(widget.code);
      if (mounted) setState(() { info = p; error = null; });
    } catch (e) {
      if (mounted) setState(() => error = e);
    }
  }

  void _onOrder(OrderView o) {
    if (!o.waitingPayment && mounted) _done(o);
  }

  void _done(OrderView o) {
    Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => TrackScreen(code: o.code)));
  }

  Future<void> _confirm() async {
    final s = context.read<AppState>();
    if (!RegExp(r'^\d{6}$').hasMatch(sms.text.trim())) {
      showInfo(context, s.t('err_WRONG_CODE'));
      return;
    }
    setState(() => sending = true);
    final ok = await busy(context, () => s.confirmVodafone(widget.code, sms.text.trim()).then((_) => true));
    if (!mounted) return;
    setState(() => sending = false);
    if (ok == true) {
      _done(await s.order(widget.code));
    } else {
      _load();
    }
  }

  Future<void> _sandboxPay() async {
    final s = context.read<AppState>();
    final ok = await busy(context, () => s.sandboxPay(widget.code).then((_) => true));
    if (ok == true && mounted) _done(await s.order(widget.code));
  }

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    final p = info;
    Widget body;
    if (error != null) {
      body = EmptyState(Icons.error_outline, s.errorText(error!), action: FilledButton(onPressed: _load, child: Text(s.t('retry'))));
    } else if (p == null) {
      body = const Center(child: CircularProgressIndicator());
    } else if (p.status != 'PENDING') {
      body = EmptyState(
        p.status == 'PAID' ? Icons.check_circle : Icons.timer_off,
        p.status == 'PAID' ? s.t('paid') : s.t('paymentExpired'),
        action: FilledButton(
            onPressed: () => Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => TrackScreen(code: widget.code))),
            child: Text(s.t('track'))),
      );
    } else {
      final left = p.expiresAt == null ? Duration.zero : p.expiresAt!.difference(DateTime.now());
      final mm = left.isNegative ? '00:00' : '${left.inMinutes.toString().padLeft(2, '0')}:${(left.inSeconds % 60).toString().padLeft(2, '0')}';
      final vf = p.method == 'VODAFONE_CASH';
      body = ListView(padding: const EdgeInsets.all(20), children: [
        Center(
          child: CircleAvatar(
            radius: 38,
            backgroundColor: vf ? const Color(0xFFE60000) : const Color(0xFFFFC20E),
            child: Icon(vf ? Icons.phone_iphone : Icons.storefront, size: 38, color: vf ? Colors.white : Colors.black87),
          ),
        ),
        const SizedBox(height: 14),
        Text(vf ? s.t('vfTitle') : s.t('fawryTitle'), textAlign: TextAlign.center, style: tt.titleLarge?.copyWith(fontWeight: FontWeight.w900)),
        const SizedBox(height: 4),
        Text(vf ? '${s.t('vfSub')} ${p.walletPhone ?? ''}' : s.t('fawrySub'),
            textAlign: TextAlign.center, style: TextStyle(color: cs.onSurfaceVariant)),
        const SizedBox(height: 18),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(children: [
              PriceRow(s.t('total'), s.money(p.amount), bold: true),
              PriceRow(s.t('timeLeft'), mm, color: left.inMinutes < 2 ? cs.error : null),
            ]),
          ),
        ),
        const SizedBox(height: 16),
        if (vf) ...[
          TextField(
            controller: sms,
            keyboardType: TextInputType.number,
            maxLength: 6,
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 28, letterSpacing: 10, fontWeight: FontWeight.w900),
            inputFormatters: [FilteringTextInputFormatter.digitsOnly],
            decoration: const InputDecoration(counterText: '', hintText: '••••••'),
          ),
          const SizedBox(height: 12),
          FilledButton(
            onPressed: sending ? null : _confirm,
            child: sending ? const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2.5)) : Text(s.t('confirm')),
          ),
          if (p.sandbox && p.sandboxCode != null)
            Padding(
              padding: const EdgeInsets.only(top: 12),
              child: Text('${s.t('sandboxHint')} ${p.sandboxCode}', textAlign: TextAlign.center, style: TextStyle(color: cs.tertiary)),
            ),
        ] else ...[
          Text(s.t('reference'), textAlign: TextAlign.center),
          const SizedBox(height: 6),
          SelectableText(p.reference ?? '', textAlign: TextAlign.center, style: const TextStyle(fontSize: 34, fontWeight: FontWeight.w900, letterSpacing: 4)),
          TextButton.icon(
            onPressed: () {
              Clipboard.setData(ClipboardData(text: p.reference ?? ''));
              showInfo(context, s.t('copied'));
            },
            icon: const Icon(Icons.copy),
            label: Text(s.t('copy')),
          ),
          if (p.sandbox) OutlinedButton(onPressed: _sandboxPay, child: Text(s.t('sandboxPay'))),
        ],
      ]);
    }
    return Scaffold(appBar: AppBar(title: Text('${s.t('payNow')} · ${widget.code}')), body: body);
  }
}
