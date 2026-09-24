import 'package:flutter/material.dart';

/// Lightweight AR/EN localization matching the web's i18n keys style.
/// Defaults to Arabic (RTL) — the storefront's primary language.
class AppLocale {
  AppLocale._();

  static const String ar = 'ar';
  static const String en = 'en';

  static String current = ar;

  static String get lang => current;

  static bool get isAr => current == ar;

  static String t(String arText, String enText) => isAr ? arText : enText;

  /// Amount formatting uses Western Arabic numerals in both languages.
  static String money(num v) =>
      '${v.toStringAsFixed(v % 1 == 0 ? 0 : 2)} EGP';

  static String tr(String key) {
    final table = isAr ? _ar : _en;
    return table[key] ?? key;
  }

  static const Map<String, String> _ar = {
    // App
    'app.name': 'VELOX',
    'app.tagline': 'السرعة القصوى تجتمع مع الفخامة الرقمية',
    'app.searchHint': 'ابحث عن وجبة، منتج إلكتروني، أو قطعة أزياء...',
    'app.go': 'انطلق',
    'app.location': 'الموقع',
    'app.cart': 'السلة',

    // Hero / home
    'home.velocity': 'التوصيل الأسرع — من 15 إلى 35 دقيقة لجميع الفئات',
    'home.body':
        'تصفح آلاف المطاعم، دور الأزياء العالمية، وأحدث التقنيات الإلكترونية. نصل إليك بأسطول رقمي ذكي مع تتبع لحظي.',
    'home.liveTracking': 'تتبع حي بدقة المتر',
    'home.guarantee': 'ضمان استرجاع بدون أسئلة',
    'home.avgTime': 'متوسط وصول: 21 دقيقة',
    'home.membersDeal': 'عرض حصري للأعضاء',
    'home.discount': 'خصم 30% على طلبك الأول',
    'home.promoDesc':
        'ينطبق على جميع أقسام المطاعم الفاخرة والأجهزة التكنولوجية الحديثة مع توصيل أولوية فائق السرعة مجاناً.',
    'home.promoCode': 'رمز القسيمة الترويجي',
    'home.copyCode': 'نسخ الكود',
    'home.copied': 'منسوخ',
    'home.pillars': 'الركائز الأساسية',
    'home.pillarsTitle': 'ثلاث منظومات تسوق متكاملة، محطة واحدة',
    'home.pillarsDesc': 'اختر مسارك السريع واكتشف آلاف المتاجر الحصرية بأعلى معايير الجودة والسرعة اللحظية.',
    'home.pillarFood': 'المطاعم والوجبات السريعة',
    'home.pillarFoodDesc': 'أفخم المطابخ العالمية والبرجر المحضر بحرفية يصلك في حافظات حرارية ذكية.',
    'home.pillarFashion': 'الأزياء والموضة',
    'home.pillarFashionDesc': 'أحدث صيحات الموضة العالمية يتم تسليمها مع استبدال فوري عند الباب.',
    'home.pillarTech': 'الإلكترونيات والتقنية',
    'home.pillarTechDesc': 'أحدث الأجهزة التقنية بأعلى معايير الجودة والضمان.',
    'home.recommended': 'مختارات لك',
    'home.recommendedSub': 'بناءً على تفضيلاتك وموقعك',
    'home.featuredStores': 'متاجر مميزة',
    'home.viewAll': 'عرض الكل',

    // Navigation
    'nav.home': 'الرئيسية',
    'nav.browse': 'استكشاف',
    'nav.cart': 'السلة',
    'nav.orders': 'طلباتي',
    'nav.account': 'حسابي',

    // Categories
    'cat.all': 'كل المختارات',
    'cat.food': 'أكل و مشروبات',
    'cat.fashion': 'أزياء و إكسسوارات',
    'cat.electronics': 'إلكترونيات و تقنية',

    // Product / cart
    'pdt.add': 'أضف',
    'pdt.added': 'تمت الإضافة للسلة',
    'pdt.qty': 'الكمية',
    'cart.empty': 'سلتك فارغة',
    'cart.emptyHint': 'أضف منتجات للبدء في الطلب',
    'cart.browse': 'تصفح المنتجات',
    'cart.total': 'الإجمالي',
    'cart.subtotal': 'المنتجات',
    'cart.shipping': 'التوصيل',
    'cart.discount': 'الخصم',
    'cart.checkout': 'إتمام الطلب',
    'cart.remove': 'إزالة',

    // Checkout
    'co.title': 'إتمام الطلب',
    'co.stepAddress': 'العنوان',
    'co.stepPayment': 'الدفع',
    'co.stepConfirm': 'التأكيد',
    'co.name': 'الاسم',
    'co.nameAuto': 'يجلب تلقائياً من حسابك ويمكنك تعديله',
    'co.phone': 'رقم الهاتف',
    'co.address': 'العنوان التفصيلي (الشارع - المنطقة - الدور)',
    'co.governorate': 'المحافظة',
    'co.governorateAuto': 'تحديد تلقائي من موقعك',
    'co.governorateDetected': 'تم تحديد محافظتك تلقائياً',
    'co.payment': 'طريقة الدفع',
    'co.cod': 'الدفع عند الاستلام',
    'co.wallet': 'محفظة VELOX',
    'co.visa': 'بطاقة بنكية',
    'co.etaFood': 'يصل الطلب خلال',
    'co.etaDays': 'يصل الطلب خلال',
    'co.minutes': 'دقيقة',
    'co.hour': 'ساعة',
    'co.day': 'يوم',
    'co.days': 'أيام',
    'co.place': 'تأكيد الطلب',
    'co.success': 'تم استلام طلبك بنجاح!',
    'co.orderCode': 'رقم الطلب',
    'co.track': 'تتبع الطلب',
    'co.continueShopping': 'متابعة التسوق',
    'co.loginRequired': 'يجب تسجيل الدخول أولاً لإتمام الطلب',
    'co.cardNumber': 'رقم البطاقة',
    'co.cardHolder': 'اسم حامل البطاقة',
    'co.expiry': 'تاريخ الانتهاء',
    'co.cvv': 'CVV',

    // Auth
    'auth.welcome': 'مرحباً بك في VELOX',
    'auth.subtitle': 'سجّل دخولك لمتابعة طلباتك المفضلة',
    'auth.login': 'تسجيل الدخول',
    'auth.register': 'حساب جديد',
    'auth.email': 'البريد الإلكتروني',
    'auth.password': 'كلمة المرور',
    'auth.fullName': 'الاسم الكامل',
    'auth.phone': 'رقم الهاتف',
    'auth.governorate': 'المحافظة',
    'auth.haveAccount': 'لديك حساب؟',
    'auth.noAccount': 'ليس لديك حساب؟',
    'auth.otpTitle': 'أدخل رمز التحقق',
    'auth.otpSent': 'أرسلنا رمزاً إلى',
    'auth.otpPlaceholder': 'رمز التحقق',
    'auth.verify': 'تحقق',
    'auth.resend': 'إعادة إرسال الرمز',
    'auth.google': 'متابعة بحساب Google',
    'auth.otpHint': 'في وضع العرض، استخدم الرمز الظاهر في رسالة التسجيل',
    'auth.logout': 'تسجيل الخروج',
    'auth.error.invalid': 'بيانات الدخول غير صحيحة',
    'auth.error.taken': 'البريد مستخدم من قبل',
    'auth.error.otp': 'الرمز غير صحيح',

    // Account / settings
    'acct.title': 'حسابي',
    'acct.profile': 'الملف الشخصي',
    'acct.settings': 'الإعدادات',
    'acct.orders': 'طلباتي',
    'acct.address': 'العناوين المحفوظة',
    'acct.favorites': 'المفضلة',
    'acct.notifications': 'الإشعارات',
    'acct.plus': 'VELOX Plus',
    'acct.rewards': 'المكافآت',
    'acct.support': 'الدعم الفني',
    'acct.version': 'الإصدار 1.0.0',
    'acct.notLoggedIn': 'غير مسجل الدخول',
    'acct.loginPrompt': 'سجّل الدخول للوصول لطلباتك ومحتواك المخصص',
    'set.phone': 'رقم الهاتف',
    'set.phoneHint': 'حدّث رقمك لاستخدامه في التوصيل',
    'set.save': 'حفظ التغييرات',
    'set.saved': 'تم الحفظ',
    'set.language': 'اللغة',
    'set.arabic': 'العربية',
    'set.english': 'English',
    'set.logoutTitle': 'تسجيل الخروج',
    'set.logoutConfirm': 'متأكد من تسجيل الخروج؟',

    // Orders
    'ord.title': 'طلباتي',
    'ord.empty': 'لا توجد طلبات بعد',
    'ord.emptyHint': 'ابدأ أول طلب اليوم!',
    'ord.order': 'طلب',
    'ord.track': 'تتبع',
    'ord.status': 'الحالة',

    // Misc
    'misc.loading': 'جارِ التحميل...',
    'misc.retry': 'إعادة المحاولة',
    'misc.error': 'حدث خطأ',
    'misc.offline': 'أنت في وضع عدم الاتصال — تعرض بيانات تجريبية',
    'misc.cash': 'نقدي عند الاستلام',
  };

  static const Map<String, String> _en = {
    'app.name': 'VELOX',
    'app.tagline': 'Maximum velocity meets digital luxury',
    'app.searchHint': 'Search a meal, gadget or fashion piece...',
    'app.go': 'Go',
    'app.location': 'Location',
    'app.cart': 'Cart',

    'home.velocity': 'The fastest delivery — 15 to 35 minutes for every category',
    'home.body': 'Browse thousands of restaurants, global fashion houses, and the latest electronics. Delivered by a smart digital fleet with live tracking.',
    'home.liveTracking': 'Meter-precise live tracking',
    'home.guarantee': 'No-questions returns',
    'home.avgTime': 'Average arrival: 21 minutes',
    'home.membersDeal': 'Exclusive members offer',
    'home.discount': '30% OFF your first order',
    'home.promoDesc': 'Applies to all fine dining and latest tech with free priority ultra-fast delivery.',
    'home.promoCode': 'Promo code',
    'home.copyCode': 'Copy code',
    'home.copied': 'Copied',
    'home.pillars': 'Core pillars',
    'home.pillarsTitle': 'Three complete shopping ecosystems, one stop',
    'home.pillarsDesc': 'Pick your fast lane and discover thousands of exclusive stores.',
    'home.pillarFood': 'Restaurants & fast food',
    'home.pillarFoodDesc': 'Fine world kitchens and craft burgers delivered in smart thermal packaging.',
    'home.pillarFashion': 'Fashion & streetwear',
    'home.pillarFashionDesc': 'The latest global trends delivered with instant door swap.',
    'home.pillarTech': 'Electronics & tech',
    'home.pillarTechDesc': 'The newest devices with the highest quality and warranty standards.',
    'home.recommended': 'Picked for you',
    'home.recommendedSub': 'Based on your preferences and location',
    'home.featuredStores': 'Featured stores',
    'home.viewAll': 'View all',

    'nav.home': 'Home',
    'nav.browse': 'Explore',
    'nav.cart': 'Cart',
    'nav.orders': 'Orders',
    'nav.account': 'Account',

    'cat.all': 'All picks',
    'cat.food': 'Food & drinks',
    'cat.fashion': 'Fashion',
    'cat.electronics': 'Electronics',

    'pdt.add': 'Add',
    'pdt.added': 'Added to cart',
    'pdt.qty': 'Qty',
    'cart.empty': 'Your cart is empty',
    'cart.emptyHint': 'Add products to start an order',
    'cart.browse': 'Browse products',
    'cart.total': 'Total',
    'cart.subtotal': 'Items',
    'cart.shipping': 'Delivery',
    'cart.discount': 'Discount',
    'cart.checkout': 'Checkout',
    'cart.remove': 'Remove',

    'co.title': 'Checkout',
    'co.stepAddress': 'Address',
    'co.stepPayment': 'Payment',
    'co.stepConfirm': 'Confirm',
    'co.name': 'Full name',
    'co.nameAuto': 'Auto-filled from your account, editable',
    'co.phone': 'Phone',
    'co.address': 'Detailed address (street - area - floor)',
    'co.governorate': 'Governorate',
    'co.governorateAuto': 'Auto-detected from your location',
    'co.governorateDetected': 'Governorate auto-detected',
    'co.payment': 'Payment method',
    'co.cod': 'Cash on delivery',
    'co.wallet': 'VELOX Wallet',
    'co.visa': 'Bank card',
    'co.etaFood': 'Arrives within',
    'co.etaDays': 'Arrives within',
    'co.minutes': 'minutes',
    'co.hour': 'hours',
    'co.day': 'day',
    'co.days': 'days',
    'co.place': 'Place order',
    'co.success': 'Order placed successfully!',
    'co.orderCode': 'Order code',
    'co.track': 'Track order',
    'co.continueShopping': 'Continue shopping',
    'co.loginRequired': 'Please sign in to complete your order',
    'co.cardNumber': 'Card number',
    'co.cardHolder': 'Cardholder name',
    'co.expiry': 'Expiry',
    'co.cvv': 'CVV',

    'auth.welcome': 'Welcome to VELOX',
    'auth.subtitle': 'Sign in to continue to your orders and favorites',
    'auth.login': 'Sign in',
    'auth.register': 'Create account',
    'auth.email': 'Email',
    'auth.password': 'Password',
    'auth.fullName': 'Full name',
    'auth.phone': 'Phone number',
    'auth.governorate': 'Governorate',
    'auth.haveAccount': 'Already have an account?',
    'auth.noAccount': 'No account yet?',
    'auth.otpTitle': 'Enter verification code',
    'auth.otpSent': 'We sent a code to',
    'auth.otpPlaceholder': 'Verification code',
    'auth.verify': 'Verify',
    'auth.resend': 'Resend code',
    'auth.google': 'Continue with Google',
    'auth.otpHint': 'In demo mode the code is shown in the registration response',
    'auth.logout': 'Log out',
    'auth.error.invalid': 'Invalid credentials',
    'auth.error.taken': 'Email already registered',
    'auth.error.otp': 'Wrong verification code',

    'acct.title': 'Account',
    'acct.profile': 'Profile',
    'acct.settings': 'Settings',
    'acct.orders': 'My orders',
    'acct.address': 'Saved addresses',
    'acct.favorites': 'Favorites',
    'acct.notifications': 'Notifications',
    'acct.plus': 'VELOX Plus',
    'acct.rewards': 'Rewards',
    'acct.support': 'Support',
    'acct.version': 'Version 1.0.0',
    'acct.notLoggedIn': 'Not signed in',
    'acct.loginPrompt': 'Sign in to access your orders and personalized content',
    'set.phone': 'Phone number',
    'set.phoneHint': 'Update your number used for delivery',
    'set.save': 'Save changes',
    'set.saved': 'Saved',
    'set.language': 'Language',
    'set.arabic': 'العربية',
    'set.english': 'English',
    'set.logoutTitle': 'Log out',
    'set.logoutConfirm': 'Are you sure you want to log out?',

    'ord.title': 'My orders',
    'ord.empty': 'No orders yet',
    'ord.emptyHint': 'Place your first order today!',
    'ord.order': 'Order',
    'ord.track': 'Track',
    'ord.status': 'Status',

    'misc.loading': 'Loading...',
    'misc.retry': 'Retry',
    'misc.error': 'Something went wrong',
    'misc.offline': 'You are offline — showing demo data',
    'misc.cash': 'Cash on delivery',
  };
}

/// Directionality helpers.
bool isRtl() => AppLocale.isAr;

TextDirection appDirection() => isRtl() ? TextDirection.rtl : TextDirection.ltr;