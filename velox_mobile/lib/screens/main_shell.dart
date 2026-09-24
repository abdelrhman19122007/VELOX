import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/locale.dart';
import '../services/cart_state.dart';
import '../theme/app_theme.dart';
import 'account_screen.dart';
import 'browse_screen.dart';
import 'cart_screen.dart';
import 'home_screen.dart';
import 'orders_screen.dart';

class MainShell extends StatefulWidget {
  const MainShell({super.key});

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  int _index = 0;

  @override
  Widget build(BuildContext context) {
    final pages = [
      const HomeScreen(),
      const BrowseScreen(),
      const CartScreen(),
      const OrdersScreen(),
      const AccountScreen(),
    ];

    return Scaffold(
      body: IndexedStack(index: _index, children: pages),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _index,
        onTap: (i) => setState(() => _index = i),
        items: [
          BottomNavigationBarItem(
            icon: const Icon(Icons.home_outlined),
            activeIcon: const Icon(Icons.home),
            label: AppLocale.tr('nav.home'),
          ),
          BottomNavigationBarItem(
            icon: const Icon(Icons.explore_outlined),
            activeIcon: const Icon(Icons.explore),
            label: AppLocale.tr('nav.browse'),
          ),
          BottomNavigationBarItem(
            icon: _CartBadge(),
            label: AppLocale.tr('nav.cart'),
          ),
          BottomNavigationBarItem(
            icon: const Icon(Icons.receipt_long_outlined),
            activeIcon: const Icon(Icons.receipt_long),
            label: AppLocale.tr('nav.orders'),
          ),
          BottomNavigationBarItem(
            icon: const Icon(Icons.person_outline),
            activeIcon: const Icon(Icons.person),
            label: AppLocale.tr('nav.account'),
          ),
        ],
      ),
    );
  }
}

class _CartBadge extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Consumer<CartState>(
      builder: (_, cart, __) {
        final count = cart.count;
        return Badge(
          isLabelVisible: count > 0,
          label: Text('$count'),
          backgroundColor: AppColors.primary,
          child: const Icon(Icons.shopping_bag_outlined),
        );
      },
    );
  }
}