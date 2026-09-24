import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:provider/provider.dart';

import 'core/locale.dart';
import 'screens/splash_screen.dart';
import 'services/auth_service.dart';
import 'services/cart_state.dart';
import 'services/catalog_service.dart';
import 'theme/app_theme.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const VeloxApp());
}

class VeloxApp extends StatefulWidget {
  const VeloxApp({super.key});

  @override
  State<VeloxApp> createState() => _VeloxAppState();
}

class _VeloxAppState extends State<VeloxApp> {
  String _lang = AppLocale.ar;

  @override
  void initState() {
    super.initState();
    AuthServiceState.instance.addListener(_onLangChanged);
  }

  @override
  void dispose() {
    AuthServiceState.instance.removeListener(_onLangChanged);
    super.dispose();
  }

  void _onLangChanged() {
    final l = AppLocale.current;
    if (l != _lang) setState(() => _lang = l);
  }

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider<CartState>.value(value: CartState.instance),
        ChangeNotifierProvider<AuthServiceState>.value(
            value: AuthServiceState.instance),
        ChangeNotifierProvider<CatalogLoading>.value(
            value: CatalogLoading.instance),
      ],
      child: MaterialApp(
        title: 'VELOX',
        debugShowCheckedModeBanner: false,
        theme: buildVeloxTheme(lang: _lang),
        locale: _lang == 'ar' ? const Locale('ar') : const Locale('en'),
        supportedLocales: const [Locale('ar'), Locale('en')],
        localizationsDelegates: const [
          GlobalMaterialLocalizations.delegate,
          GlobalWidgetsLocalizations.delegate,
          GlobalCupertinoLocalizations.delegate,
        ],
        builder: (context, child) => Directionality(
          textDirection: appDirection(),
          child: child!,
        ),
        home: const SplashScreen(),
      ),
    );
  }
}

/// App-level controller for auth/profile + language.
class AuthServiceState extends ChangeNotifier {
  AuthServiceState._();

  static final AuthServiceState instance = AuthServiceState._();

  final _auth = AuthService.instance;

  bool get isLoggedIn => _auth.isLoggedIn;

  dynamic get profile => _auth.profile;

  Future<void> restore() async => _auth.restore();

  void notify() => notifyListeners();

  Future<void> setLang(String lang) async {
    AppLocale.current = lang;
    notifyListeners();
  }
}

/// Catalog loading progress used by the splash + screens.
class CatalogLoading extends ChangeNotifier {
  CatalogLoading._();

  static final CatalogLoading instance = CatalogLoading._();

  bool _ready = false;

  bool get ready => _ready;

  Future<void> ensureLoaded() async {
    if (_ready) return;
    await CatalogService.instance.load();
    _ready = true;
    notifyListeners();
  }
}