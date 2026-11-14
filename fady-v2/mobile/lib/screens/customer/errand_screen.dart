import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/app_state.dart';
import '../../widgets/common.dart';
import 'checkout_screen.dart';

/// "Bring me anything": the customer writes what they need; a driver buys it from any shop in town.
class ErrandScreen extends StatefulWidget {
  const ErrandScreen({super.key});
  @override
  State<ErrandScreen> createState() => _ErrandScreenState();
}

class _ErrandScreenState extends State<ErrandScreen> {
  final details = TextEditingController();
  bool loading = false;

  Future<void> _next() async {
    final s = context.read<AppState>();
    final text = details.text.trim();
    if (text.length < 3 || RegExp(r'[<>]').hasMatch(text)) {
      showInfo(context, s.t('err_INVALID_ERRAND'));
      return;
    }
    setState(() => loading = true);
    final q = await busy(context, () => s.quote(errand: true));
    if (!mounted) return;
    setState(() => loading = false);
    if (q != null) {
      Navigator.push(context, MaterialPageRoute(builder: (_) => CheckoutScreen(quote: q, errandDetails: text)));
    }
  }

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: Text(s.t('errand'))),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        CircleAvatar(radius: 40, backgroundColor: cs.secondaryContainer, child: Icon(Icons.shopping_basket, size: 40, color: cs.secondary)),
        const SizedBox(height: 14),
        Text(s.t('errandSub'), textAlign: TextAlign.center, style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 18),
        TextField(
          controller: details,
          maxLines: 6,
          maxLength: 1000,
          decoration: InputDecoration(labelText: s.t('errandDetails'), hintText: s.t('errandDetailsHint'), alignLabelWithHint: true),
        ),
        const SizedBox(height: 6),
        Text(s.t('errandNote'), style: TextStyle(color: cs.onSurfaceVariant)),
      ]),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 4, 16, 10),
          child: FilledButton(
            onPressed: loading ? null : _next,
            child: loading ? const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2.5)) : Text(s.t('next')),
          ),
        ),
      ),
    );
  }
}
