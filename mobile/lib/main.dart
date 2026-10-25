import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:provider/provider.dart';

import 'core/app_state.dart';
import 'core/theme.dart';
import 'screens/admin/admin_shell.dart';
import 'screens/auth/welcome_screen.dart';
import 'screens/customer/customer_shell.dart';
import 'screens/driver/driver_shell.dart';
import 'screens/store/store_shell.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(ChangeNotifierProvider(create: (_) => AppState()..init(), child: const VeloxApp()));
}

class VeloxApp extends StatelessWidget {
  const VeloxApp({super.key});

  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    return MaterialApp(
      title: 'VELOX',
      debugShowCheckedModeBanner: false,
      locale: Locale(s.lang),
      supportedLocales: const [Locale('ar'), Locale('en')],
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      theme: buildTheme(Brightness.light, s.lang),
      darkTheme: buildTheme(Brightness.dark, s.lang),
      themeMode: s.themeMode,
      // One app: the screens depend on the signed-in account's role.
      home: !s.ready
          ? const _Splash()
          : s.user == null
              ? const WelcomeScreen()
              : switch (s.user!.role) {
                  'RESTAURANT' => const StoreShell(),
                  'DRIVER' => const DriverShell(),
                  'ADMIN' => const AdminShell(),
                  _ => const CustomerShell(),
                },
    );
  }
}

class _Splash extends StatelessWidget {
  const _Splash();
  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      backgroundColor: cs.primary,
      body: Center(child: Icon(Icons.delivery_dining, size: 72, color: cs.onPrimary)),
    );
  }
}
