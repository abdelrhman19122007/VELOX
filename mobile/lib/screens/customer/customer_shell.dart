import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/app_state.dart';
import 'account_screen.dart';
import 'cart_screen.dart';
import 'home_screen.dart';
import 'orders_screen.dart';

/// The customer's app: Home · Orders · Cart · Account (the same four tabs people know from the big apps).
class CustomerShell extends StatefulWidget {
  const CustomerShell({super.key});
  @override
  State<CustomerShell> createState() => _CustomerShellState();
}

class _CustomerShellState extends State<CustomerShell> {
  @override
  Widget build(BuildContext context) {
    final s = context.watch<AppState>();
    final activeOrders = s.myOrders.where((o) => o.active || o.waitingPayment).length;
    return Scaffold(
      body: IndexedStack(index: s.tab, children: const [HomeScreen(), OrdersScreen(), CartScreen(), AccountScreen()]),
      bottomNavigationBar: NavigationBar(
        selectedIndex: s.tab,
        onDestinationSelected: s.goTab,
        destinations: [
          NavigationDestination(icon: const Icon(Icons.home_outlined), selectedIcon: const Icon(Icons.home), label: s.t('home')),
          NavigationDestination(
            icon: Badge(isLabelVisible: activeOrders > 0, label: Text('$activeOrders'), child: const Icon(Icons.receipt_long_outlined)),
            selectedIcon: const Icon(Icons.receipt_long),
            label: s.t('orders'),
          ),
          NavigationDestination(
            icon: Badge(isLabelVisible: s.cartCount > 0, label: Text('${s.cartCount}'), child: const Icon(Icons.shopping_bag_outlined)),
            selectedIcon: const Icon(Icons.shopping_bag),
            label: s.t('cart'),
          ),
          NavigationDestination(
            icon: Badge(isLabelVisible: s.unread > 0, child: const Icon(Icons.person_outline)),
            selectedIcon: const Icon(Icons.person),
            label: s.t('account'),
          ),
        ],
      ),
    );
  }
}
