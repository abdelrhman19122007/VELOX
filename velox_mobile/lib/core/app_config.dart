/// Global runtime configuration for the VELOX mobile app.
library;

/// Backend origin. On-device builds (Android emulator/device) must point to
/// the machine that runs the Spring Boot backend. Override via
/// `--dart-define=API_BASE_URL=http://192.168.x.x:8081/api` at build time.
const String apiBaseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'http://localhost:8081/api',
);

/// Enable demo mode: cart/checkout work fully offline against the built-in
/// catalog and the auth mock store (mirrors the web's USE_MOCK_API flag).
const bool useMockApi = false;

/// Google OAuth client id (optional web/Android client for VELOX).
const String googleClientId = String.fromEnvironment(
  'GOOGLE_CLIENT_ID',
  defaultValue: '',
);

/// Fake waiting used only when [useMockApi] is true (matches web latency).
const int mockLatencyMs = 450;

class AppEndpoints {
  static const login = '/auth/login';
  static const register = '/auth/register';
  static const verifyOtp = '/auth/verify-otp';
  static const resendOtp = '/auth/resend-otp';
  static const profile = '/auth/profile';
  static const logout = '/auth/logout';
  static const products = '/products';
  static const popular = '/products/popular';
  static const categories = '/categories';
  static const stores = '/stores';
  static const orders = '/orders';
  static const orderQuote = '/orders/quote';
  static const orderHistory = '/orders/history';
  static const couponsValidate = '/coupons/validate';
  static const governorates = '/governorates';
  static const walletBalance = '/wallet/balance';
  static const walletTopup = '/wallet/topup';
  static const loyaltyStatus = '/loyalty/status';
}