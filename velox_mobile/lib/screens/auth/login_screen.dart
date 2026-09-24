import 'package:flutter/material.dart';

import '../../core/api_client.dart';
import '../../core/locale.dart';
import '../../main.dart';
import '../../services/auth_service.dart';
import '../../theme/app_theme.dart';
import '../../widgets/glass_card.dart';
import '../../widgets/primary_button.dart';
import '../main_shell.dart';
import 'register_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _emailCtrl = TextEditingController();
  final _passwordCtrl = TextEditingController();
  final _formKey = GlobalKey<FormState>();
  bool _loading = false;
  bool _obscure = true;
  String? _error;

  @override
  void dispose() {
    _emailCtrl.dispose();
    _passwordCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      await AuthService.instance.login(
        email: _emailCtrl.text,
        password: _passwordCtrl.text,
      );
      AuthServiceState.instance.notify();
      if (!mounted) return;
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(builder: (_) => const MainShell()),
        (route) => false,
      );
    } catch (e) {
      final msg =
          e is ApiException ? e.message : AppLocale.tr('auth.error.invalid');
      setState(() {
        _error = msg;
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Stack(
        children: [
          const AmbientGlow(
              color: AppColors.primary,
              alignment: Alignment.topRight,
              size: 380),
          const AmbientGlow(
              color: AppColors.secondary,
              alignment: Alignment.bottomLeft,
              size: 300),
          SafeArea(
            child: Center(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(24),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    // brand
                    Center(
                      child: Image.asset(
                        'assets/images/velox-logo.jpeg',
                        width: 72,
                        height: 72,
                        fit: BoxFit.cover,
                        errorBuilder: (_, __, ___) => Container(
                          width: 72,
                          height: 72,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            border:
                                Border.all(color: AppColors.primary, width: 2),
                          ),
                          child: const Icon(Icons.bolt,
                              color: AppColors.primary, size: 34),
                        ),
                      ),
                    ),
                    const SizedBox(height: 18),
                    Text(
                      AppLocale.tr('auth.welcome'),
                      style: Theme.of(context).textTheme.headlineMedium,
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 6),
                    Text(
                      AppLocale.tr('auth.subtitle'),
                      style: const TextStyle(
                          color: AppColors.textMuted, fontSize: 13),
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 26),
                    GlassCard(
                      padding: const EdgeInsets.all(20),
                      child: Form(
                        key: _formKey,
                        child: Column(
                          children: [
                            TextFormField(
                              controller: _emailCtrl,
                              keyboardType: TextInputType.emailAddress,
                              style: const TextStyle(color: AppColors.textHigh),
                              decoration: InputDecoration(
                                labelText: AppLocale.tr('auth.email'),
                                prefixIcon: const Icon(Icons.email_outlined,
                                    color: AppColors.textMuted),
                              ),
                              validator: (v) =>
                                  (v == null || !v.contains('@'))
                                      ? AppLocale.t(
                                          'بريد إلكتروني غير صحيح',
                                          'Invalid email')
                                      : null,
                            ),
                            const SizedBox(height: 14),
                            TextFormField(
                              controller: _passwordCtrl,
                              obscureText: _obscure,
                              style: const TextStyle(color: AppColors.textHigh),
                              decoration: InputDecoration(
                                labelText: AppLocale.tr('auth.password'),
                                prefixIcon: const Icon(Icons.lock_outline,
                                    color: AppColors.textMuted),
                                suffixIcon: IconButton(
                                  icon: Icon(
                                      _obscure
                                          ? Icons.visibility_off_outlined
                                          : Icons.visibility_outlined,
                                      color: AppColors.textDisabled,
                                      size: 20),
                                  onPressed: () =>
                                      setState(() => _obscure = !_obscure),
                                ),
                              ),
                              validator: (v) => (v == null || v.length < 6)
                                  ? AppLocale.t(
                                      '6 أحرف على الأقل', 'Min 6 characters')
                                  : null,
                            ),
                            if (_error != null) ...[
                              const SizedBox(height: 12),
                              Container(
                                width: double.infinity,
                                padding: const EdgeInsets.all(10),
                                decoration: BoxDecoration(
                                  color: AppColors.danger
                                      .withValues(alpha: .1),
                                  borderRadius: BorderRadius.circular(8),
                                  border: Border.all(
                                      color:
                                          AppColors.danger.withValues(alpha: .4)),
                                ),
                                child: Text(
                                  _error!,
                                  style: const TextStyle(
                                      fontSize: 12, color: AppColors.danger),
                                ),
                              ),
                            ],
                            const SizedBox(height: 18),
                            PrimaryButton(
                              label: AppLocale.tr('auth.login'),
                              icon: Icons.arrow_forward,
                              loading: _loading,
                              onPressed: _loading ? null : _submit,
                            ),
                            const SizedBox(height: 10),
                            const _DividerWith(text: 'أو'),
                            const SizedBox(height: 10),
                            PrimaryButton(
                              label: AppLocale.tr('auth.google'),
                              icon: Icons.g_mobiledata,
                              variant: 'secondary',
                              onPressed: _loading
                                  ? null
                                  : () => ScaffoldMessenger.of(context)
                                        ..hideCurrentSnackBar()
                                        ..showSnackBar(SnackBar(
                                          content: Text(AppLocale.t(
                                              'تسجيل دخول جوجل غير مفعّل في نسخة الموبايل — استخدم البريد وكلمة المرور',
                                              'Google sign-in uses the web OAuth client — email/password works here')),
                                        )),
                            ),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 18),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          AppLocale.tr('auth.noAccount'),
                          style: const TextStyle(
                              color: AppColors.textMuted, fontSize: 13),
                        ),
                        TextButton(
                          onPressed: () => Navigator.of(context).push(
                            MaterialPageRoute(
                                builder: (_) => const RegisterScreen()),
                          ),
                          child: Text(
                            AppLocale.tr('auth.register'),
                            style: const TextStyle(
                                color: AppColors.primary,
                                fontWeight: FontWeight.w700),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _DividerWith extends StatelessWidget {
  final String text;

  const _DividerWith({required this.text});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        const Expanded(child: Divider()),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 10),
          child: Text(text,
              style:
                  const TextStyle(fontSize: 12, color: AppColors.textDisabled)),
        ),
        const Expanded(child: Divider()),
      ],
    );
  }
}