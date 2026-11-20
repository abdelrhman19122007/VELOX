import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/app_state.dart';

/// Three intro slides shown the first time the app opens on a device, like most delivery apps.
class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key});
  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  final page = PageController();
  int current = 0;

  static const _slides = [
    (Icons.restaurant_menu, 'ob1Title', 'ob1Body'),
    (Icons.delivery_dining, 'ob2Title', 'ob2Body'),
    (Icons.payments_outlined, 'ob3Title', 'ob3Body'),
  ];

  @override
  void dispose() {
    page.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    final last = current == _slides.length - 1;
    return Scaffold(
      appBar: AppBar(actions: [
        TextButton(onPressed: s.toggleLang, child: Text(s.t('language'))),
        TextButton(onPressed: s.finishOnboarding, child: Text(s.t('skip'))),
      ]),
      body: SafeArea(
        child: Column(children: [
          Expanded(
            child: PageView.builder(
              controller: page,
              itemCount: _slides.length,
              onPageChanged: (i) => setState(() => current = i),
              itemBuilder: (c, i) {
                final (icon, title, body) = _slides[i];
                return Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 32),
                  child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
                    if (i == 0)
                      Image.asset('assets/images/velox_logo.png', height: 150)
                    else
                      Container(
                        width: 150,
                        height: 150,
                        decoration: BoxDecoration(color: cs.primaryContainer, shape: BoxShape.circle),
                        child: Icon(icon, size: 72, color: cs.primary),
                      ),
                    const SizedBox(height: 32),
                    Text(s.t(title), textAlign: TextAlign.center, style: tt.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
                    const SizedBox(height: 12),
                    Text(s.t(body), textAlign: TextAlign.center, style: tt.bodyLarge?.copyWith(color: cs.onSurfaceVariant)),
                  ]),
                );
              },
            ),
          ),
          Row(mainAxisAlignment: MainAxisAlignment.center, children: [
            for (var i = 0; i < _slides.length; i++)
              AnimatedContainer(
                duration: const Duration(milliseconds: 250),
                margin: const EdgeInsets.symmetric(horizontal: 4),
                width: i == current ? 22 : 8,
                height: 8,
                decoration: BoxDecoration(color: i == current ? cs.primary : cs.outlineVariant, borderRadius: BorderRadius.circular(4)),
              ),
          ]),
          Padding(
            padding: const EdgeInsets.fromLTRB(24, 20, 24, 24),
            child: SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: last
                    ? s.finishOnboarding
                    : () => page.nextPage(duration: const Duration(milliseconds: 350), curve: Curves.easeOutCubic),
                child: Text(s.t(last ? 'start' : 'next')),
              ),
            ),
          ),
        ]),
      ),
    );
  }
}
