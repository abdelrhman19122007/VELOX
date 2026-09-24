import 'package:flutter/material.dart';

import '../../core/api_client.dart';
import '../../core/locale.dart';
import '../../services/auth_service.dart';
import '../../services/checkout_logic.dart';
import '../../theme/app_theme.dart';
import '../../widgets/glass_card.dart';
import '../../widgets/primary_button.dart';
import 'otp_screen.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameCtrl = TextEditingController();
  final _emailCtrl = TextEditingController();
  final _phoneCtrl = TextEditingController();
  final _passwordCtrl = TextEditingController();
  String _gov = 'DAMIETTA';
  bool _loading = false;
  bool _obscure = true;
  String? _error;

  @override
  void dispose() {
    _nameCtrl.dispose();
    _emailCtrl.dispose();
    _phoneCtrl.dispose();
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
      final res = await AuthService.instance.register(
        email: _emailCtrl.text,
        password: _passwordCtrl.text,
        fullName: _nameCtrl.text,
        phone: _phoneCtrl.text,
        governorate: _gov,
      );
      if (!mounted) return;
      if (res['pending'] == true) {
        // OTP verification step (matches the web flow).
        Navigator.of(context).pushReplacement(MaterialPageRoute(
          builder: (_) => OtpScreen(
            email: res['email'] as String,
            demoOtp: (res['otp'] as String?) ?? '',
          ),
        ));
      } else {
        Navigator.of(context).pop();
      }
    } catch (e) {
      final msg = e is ApiException ? e.message : '$e';
      setState(() {
        _error = msg;
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.canvas,
      appBar: AppBar(title: Text(AppLocale.tr('auth.register'))),
      body: Stack(
        children: [
          const AmbientGlow(
              color: AppColors.tertiary,
              alignment: Alignment.topLeft,
              size: 320),
          SingleChildScrollView(
            padding: const EdgeInsets.all(20),
            child: GlassCard(
              padding: const EdgeInsets.all(20),
              child: Form(
                key: _formKey,
                child: Column(
                  children: [
                    TextFormField(
                      controller: _nameCtrl,
                      style: const TextStyle(color: AppColors.textHigh),
                      decoration: InputDecoration(
                        labelText: AppLocale.tr('auth.fullName'),
                        prefixIcon: const Icon(Icons.person_outline,
                            color: AppColors.textMuted),
                      ),
                      validator: (v) => (v == null || v.trim().length < 3)
                          ? AppLocale.t('أدخل اسمك الكامل', 'Enter your full name')
                          : null,
                    ),
                    const SizedBox(height: 14),
                    TextFormField(
                      controller: _emailCtrl,
                      keyboardType: TextInputType.emailAddress,
                      style: const TextStyle(color: AppColors.textHigh),
                      decoration: InputDecoration(
                        labelText: AppLocale.tr('auth.email'),
                        prefixIcon: const Icon(Icons.email_outlined,
                            color: AppColors.textMuted),
                      ),
                      validator: (v) => (v == null || !v.contains('@'))
                          ? AppLocale.t('بريد غير صحيح', 'Invalid email')
                          : null,
                    ),
                    const SizedBox(height: 14),
                    TextFormField(
                      controller: _phoneCtrl,
                      keyboardType: TextInputType.phone,
                      style: const TextStyle(color: AppColors.textHigh),
                      decoration: InputDecoration(
                        labelText: AppLocale.tr('auth.phone'),
                        prefixIcon: const Icon(Icons.phone_android,
                            color: AppColors.textMuted),
                      ),
                      validator: (v) => RegExp(r'^01\d{9}$')
                              .hasMatch((v ?? '').replaceAll(' ', ''))
                          ? null
                          : AppLocale.t('رقم مصري صحيح (01xxxxxxxxx)',
                              'Valid Egyptian number (01xxxxxxxxx)'),
                    ),
                    const SizedBox(height: 14),
                    DropdownButtonFormField<String>(
                      initialValue: _gov,
                      decoration: InputDecoration(
                        labelText: AppLocale.tr('auth.governorate'),
                        prefixIcon: const Icon(Icons.map_outlined,
                            color: AppColors.textMuted),
                      ),
                      items: CheckoutData.all
                          .map((g) => DropdownMenuItem(
                                value: g.id,
                                child: Text(g.nameAr,
                                    style: const TextStyle(fontSize: 13)),
                              ))
                          .toList(),
                      onChanged: (v) {
                        if (v != null) setState(() => _gov = v);
                      },
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
                          ? AppLocale.t('6 أحرف على الأقل', 'Min 6 characters')
                          : null,
                    ),
                    if (_error != null) ...[
                      const SizedBox(height: 12),
                      Container(
                        width: double.infinity,
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: AppColors.danger.withValues(alpha: .1),
                          borderRadius: BorderRadius.circular(8),
                          border: Border.all(
                              color:
                                  AppColors.danger.withValues(alpha: .4)),
                        ),
                        child: Text(_error!,
                            style: const TextStyle(
                                fontSize: 12, color: AppColors.danger)),
                      ),
                    ],
                    const SizedBox(height: 18),
                    PrimaryButton(
                      label: AppLocale.tr('auth.register'),
                      icon: Icons.person_add_alt,
                      loading: _loading,
                      onPressed: _loading ? null : _submit,
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