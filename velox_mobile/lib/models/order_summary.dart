/// Order summary returned by POST /api/orders and GET /api/orders/history.
class OrderSummary {
  final String orderCode;
  final int orderId;
  final String status;
  final double total;
  final String paymentMethod;
  final String? createdAt;

  const OrderSummary({
    required this.orderCode,
    required this.orderId,
    required this.status,
    required this.total,
    required this.paymentMethod,
    this.createdAt,
  });

  String get statusLabelAr => switch (status) {
        'PENDING' => 'قيد التجهيز',
        'CONFIRMED' => 'مؤكد',
        'DELIVERED' => 'تم التوصيل',
        'CANCELLED' => 'ملغي',
        _ => status,
      };

  factory OrderSummary.fromJson(Map<String, dynamic> j) => OrderSummary(
        orderCode:
            (j['orderCode'] ?? j['order_code'] ?? j['code'] ?? '').toString(),
        orderId: (j['orderId'] ?? j['order_id'] ?? j['id'] as num?)?.toInt() ??
            0,
        status: (j['status'] ?? 'PENDING').toString(),
        total: (j['total'] ?? j['total_amount'] ?? j['final_amount'] as num?)
                ?.toDouble() ??
            0,
        paymentMethod:
            (j['paymentMethod'] ?? j['payment_method'] ?? 'CASH_ON_DELIVERY')
                .toString(),
        createdAt: (j['created_at'] ?? j['orderDate'] ?? j['createdAt'])
            ?.toString(),
      );
}