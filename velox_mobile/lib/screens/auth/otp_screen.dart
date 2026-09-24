import 'package:flutter/material.dart';

import '../../core/api_client.dart';
import '../../core/locale.dart';
import '../../main.dart';
import '../../services/auth_service.dart';
import '../../theme/app_theme.dart';
import '../../widgets/glass_card.dart';
import '../../widgets/primary_button.dart';
import '../main_shell.dart';

class OtpScreen extends StatefulWidget {
  final String email;
  final String demoOtp;

  const OtpScreen({super.key, required this.email, this.demoOtp = ''});

  @override
  State<OtpScreen> createState() => _OtpScreenState();
}

class _OtpScreenState extends State<OtpScreen> {
  final _otpCtrl = TextEditingController();
  bool _loading = false;
  String? _error;
  String _hint = '';

  @override
  void initState() {
    super.initState();
    if (widget.demoOtp.isNotEmpty) {
      _hint = '${AppLocale.tr('auth.otpHint')}: ${widget.demoOtp}';
    }
  }

  @override
  void dispose() {
    _otpCtrl.dispose();
    super.dispose();
  }

  Future<void> _verify() async {
    if (_otpCtrl.text.trim().isEmpty) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      await AuthService.instance.verifyOtp(
        email: widget.email,
        code: _otpCtrl.text.trim(),
      );
      AuthServiceState.instance.notify();
      if (!mounted) return;
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(builder: (_) => const MainShell()),
        (route) => false,
      );
    } catch (e) {
      final msg =
          e is ApiException ? e.message : AppLocale.tr('auth.error.otp');
      setState(() {
        _error = msg;
        _loading = false;
      });
    }
  }

  Future<void> _resend() async {
    try {
      await AuthService.instance.resendOtp(widget.email);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(AppLocale.t('تم إرسال رمز جديد', 'New code sent'))),
      );
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.canvas,
      appBar: AppBar(title: Text(AppLocale.tr('auth.otpTitle'))),
      body: Stack(
        children: [
          const AmbientGlow(
              color: AppColors.secondary,
              alignment: Alignment.topRight,
              size: 340),
          Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const SizedBox(height: 12),
                GlassCard(
                  padding: const EdgeInsets.all(20),
                  child: Column(
                    children: [
                      Container(
                        width: 72,
                        height: 72,
                        decoration: BoxDecoration(
                          color: AppColors.secondary.withValues(alpha: .1),
                          shape: BoxShape.circle,
                          border: Border.all(color: AppColors.secondary),
                        ),
                        child: const Icon(Icons.sms_outlined,
                            color: AppColors.secondary, size: 34),
                      ),
                      const SizedBox(height: 16),
                      Text(
                        '${AppLocale.tr('auth.otpSent')} ${widget.email}',
                        textAlign: TextAlign.center,
                        style: const TextStyle(
                            fontSize: 13, color: AppColors.textMuted),
                      ),
                      if (_hint.isNotEmpty) ...[
                        const SizedBox(height: 8),
                        Text(
                          _hint,
                          textAlign: TextAlign.center,
                          style: const TextStyle(
                              fontSize: 11,
                              color: AppColors.warn,
                              fontWeight: FontWeight.w600),
                        ),
                      ],
                      const SizedBox(height: 18),
                      TextField(
                        controller: _otpCtrl,
                        textAlign: TextAlign.center,
                        keyboardType: TextInputType.number,
                        maxLength: 6,
                        style: const TextStyle(
                          fontSize: 26,
                          fontWeight: FontWeight.w800,
                          letterSpacing: 10,
                          color: AppColors.textHigh,
                        ),
                        decoration: InputDecoration(
                          counterText: '',
                          hintText: '000000',
                          hintStyle: TextStyle(
                              fontSize: 22,
                              letterSpacing: 10,
                              color: AppColors.textDisabled),
                        ),
                      ),
                      if (_error != null) ...[
                        const SizedBox(height: 10),
                        Text(_error!,
                            style: const TextStyle(
                                color: AppColors.danger, fontSize: 12)),
                      ],
                      const SizedBox(height: 18),
                      PrimaryButton(
                        label: AppLocale.tr('auth.verify'),
                        icon: Icons.verified_outlined,
                        loading: _loading,
                        onPressed: _loading ? null : _verify,
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 14),
                TextButton.icon(
                  onPressed: _resend,
                  icon: const Icon(Icons.refresh,
                      size: 16, color: AppColors.secondary),
                  label: Text(
                    AppLocale.tr('auth.resend'),
                    style: const TextStyle(
                        color: AppColors.secondary,
                        fontWeight: FontWeight.w700),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}