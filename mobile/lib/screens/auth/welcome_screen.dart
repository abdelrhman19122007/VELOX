import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/app_state.dart';
import '../../widgets/common.dart';
import '../shared/settings_screen.dart';

/// Sign in / sign up. Customers and drivers create their own accounts; store and admin accounts come from the admin.
class WelcomeScreen extends StatefulWidget {
  const WelcomeScreen({super.key});
  @override
  State<WelcomeScreen> createState() => _WelcomeScreenState();
}

class _WelcomeScreenState extends State<WelcomeScreen> {
  final _form = GlobalKey<FormState>();
  final name = TextEditingController(), email = TextEditingController(), phone = TextEditingController();
  final password = TextEditingController();
  bool signUp = false, hide = true, loading = false;

  Future<void> submit() async {
    if (!_form.currentState!.validate()) return;
    final s = context.read<AppState>();
    setState(() => loading = true);
    await busy(context, () => signUp
        ? s.register(name.text, email.text, phone.text, password.text, false)
        : s.login(email.text, password.text));
    if (mounted) setState(() => loading = false);
  }

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    return Scaffold(
      appBar: AppBar(actions: [
        TextButton(onPressed: s.toggleLang, child: Text(s.t('language'))),
        IconButton(
          tooltip: s.t('server'),
          icon: const Icon(Icons.dns_outlined),
          onPressed: () => showServerDialog(context),
        ),
      ]),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 440),
              child: Form(
                key: _form,
                child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                  Center(
                    // the logo pops in once when the screen opens
                    child: TweenAnimationBuilder<double>(
                      tween: Tween(begin: 0.6, end: 1),
                      duration: const Duration(milliseconds: 700),
                      curve: Curves.elasticOut,
                      builder: (c, v, child) => Transform.scale(scale: v, child: child),
                      child: Container(
                        width: 92,
                        height: 92,
                        decoration: BoxDecoration(color: cs.primary, borderRadius: BorderRadius.circular(28)),
                        child: Icon(Icons.delivery_dining, size: 54, color: cs.onPrimary),
                      ),
                    ),
                  ),
                  const SizedBox(height: 18),
                  Text(s.t('welcome'), textAlign: TextAlign.center, style: tt.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
                  const SizedBox(height: 4),
                  Text(s.t('tagline'), textAlign: TextAlign.center, style: tt.bodyLarge?.copyWith(color: cs.onSurfaceVariant)),
                  const SizedBox(height: 14),
                  Wrap(alignment: WrapAlignment.center, spacing: 8, runSpacing: 8, children: [
                    for (final (i, p) in [
                      (Icons.storefront, 'perk1'),
                      (Icons.location_on_outlined, 'perk2'),
                      (Icons.payments_outlined, 'perk3'),
                    ].indexed)
                      FadeSlideIn(
                        index: i + 1,
                        // a plain pill: Chip clips the Cairo font's tall line height on the web
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                          decoration: BoxDecoration(
                            color: cs.primaryContainer.withValues(alpha: 0.45),
                            borderRadius: BorderRadius.circular(20),
                          ),
                          child: Row(mainAxisSize: MainAxisSize.min, children: [
                            Icon(p.$1, size: 18, color: cs.primary),
                            const SizedBox(width: 6),
                            Text(s.t(p.$2), style: tt.labelLarge),
                          ]),
                        ),
                      ),
                  ]),
                  const SizedBox(height: 22),
                  SegmentedButton<bool>(
                    segments: [
                      ButtonSegment(value: false, label: Text(s.t('signIn'))),
                      ButtonSegment(value: true, label: Text(s.t('signUp'))),
                    ],
                    selected: {signUp},
                    onSelectionChanged: (v) => setState(() => signUp = v.first),
                  ),
                  const SizedBox(height: 20),
                  if (signUp) ...[
                    TextFormField(
                      controller: name,
                      textInputAction: TextInputAction.next,
                      decoration: InputDecoration(labelText: s.t('name'), prefixIcon: const Icon(Icons.person_outline)),
                      validator: (v) => (v ?? '').trim().length < 2 ? s.t('badName') : null,
                    ),
                    const SizedBox(height: 12),
                  ],
                  TextFormField(
                    controller: email,
                    keyboardType: TextInputType.emailAddress,
                    textInputAction: TextInputAction.next,
                    autofillHints: const [AutofillHints.email],
                    decoration: InputDecoration(labelText: s.t('email'), prefixIcon: const Icon(Icons.mail_outline)),
                    validator: (v) => RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch((v ?? '').trim()) ? null : s.t('badEmail'),
                  ),
                  const SizedBox(height: 12),
                  if (signUp) ...[
                    TextFormField(
                      controller: phone,
                      keyboardType: TextInputType.phone,
                      textInputAction: TextInputAction.next,
                      decoration: InputDecoration(labelText: s.t('phone'), prefixIcon: const Icon(Icons.phone_outlined)),
                      validator: (v) => RegExp(r'^01[0125]\d{8}$').hasMatch((v ?? '').trim()) ? null : s.t('badPhone'),
                    ),
                    const SizedBox(height: 12),
                  ],
                  TextFormField(
                    controller: password,
                    obscureText: hide,
                    autofillHints: const [AutofillHints.password],
                    onFieldSubmitted: (_) => submit(),
                    decoration: InputDecoration(
                      labelText: s.t('password'),
                      helperText: signUp ? s.t('passwordHint') : null,
                      prefixIcon: const Icon(Icons.lock_outline),
                      suffixIcon: IconButton(
                        icon: Icon(hide ? Icons.visibility_outlined : Icons.visibility_off_outlined),
                        onPressed: () => setState(() => hide = !hide),
                      ),
                    ),
                    validator: (v) => signUp && (v ?? '').length < 8 ? s.t('badPassword') : ((v ?? '').isEmpty ? s.t('badPassword') : null),
                  ),
                  if (signUp)
                    Padding(
                      padding: const EdgeInsets.only(top: 8),
                      child: Text(s.t('driverNote'), style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant)),
                    )
                  else
                    Padding(
                      padding: const EdgeInsets.only(top: 8),
                      child: Text(s.t('forgot'), style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant)),
                    ),
                  const SizedBox(height: 20),
                  FilledButton(
                    onPressed: loading ? null : submit,
                    child: loading
                        ? const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2.5))
                        : Text(signUp ? s.t('createAccount') : s.t('login')),
                  ),
                ]),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
