package com.velox.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.velox.order.domain.DeliveryZone;
import com.velox.order.domain.OrderItem;
import com.velox.order.domain.OrderKind;
import com.velox.order.domain.OrderStatus;
import com.velox.order.domain.PaymentMethod;
import com.velox.order.domain.PaymentStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** What the order endpoints receive and return. Prices sent by clients are never read — the server prices. */
public final class OrderDtos {

    private OrderDtos() {
    }

    static final String PLAIN_TEXT = "^[^<>]*$";
    static final String EG_MOBILE = "^01[0125]\\d{8}$";

    // ---------- requests ----------

    public record CartLine(@NotBlank @Size(max = 10) String productId,
                           @Min(1) @Max(99) int qty,
                           @Size(max = 200) @Pattern(regexp = PLAIN_TEXT) String note) {
    }

    public record QuoteRequest(@NotBlank String zone,
                               @Size(max = 30) List<@Valid CartLine> items,
                               OrderKind kind,
                               @Size(max = 20) String promo) {
    }

    public record PlaceOrderRequest(OrderKind kind,
                                    @NotBlank @Size(max = 30) String zone,
                                    @NotBlank @Pattern(regexp = EG_MOBILE, message = "Egyptian mobile number") String phone,
                                    @NotBlank @Size(min = 5, max = 300) @Pattern(regexp = PLAIN_TEXT) String address,
                                    @Size(max = 150) @Pattern(regexp = PLAIN_TEXT) String landmark,
                                    @DecimalMin("-90") @DecimalMax("90") Double lat,
                                    @DecimalMin("-180") @DecimalMax("180") Double lng,
                                    @Size(max = 30) List<@Valid CartLine> items,
                                    @Size(max = 1000) @Pattern(regexp = PLAIN_TEXT) String errandDetails,
                                    PaymentMethod paymentMethod,
                                    @Pattern(regexp = "^(010\\d{8})?$") String walletPhone,
                                    @DecimalMin("1") @DecimalMax("100000") BigDecimal cashChangeFor,
                                    Instant scheduledFor,
                                    @Size(max = 20) String promo) {
    }

    public record ZoneRequest(@NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,29}$") String code,
                              @NotBlank @Size(max = 60) @Pattern(regexp = PLAIN_TEXT) String nameAr,
                              @NotBlank @Size(max = 60) @Pattern(regexp = PLAIN_TEXT) String nameEn,
                              @NotBlank @Pattern(regexp = "^(CITY|VILLAGE)$") String kind,
                              @NotNull @DecimalMin("0") @DecimalMax("500") BigDecimal fee,
                              @Min(5) @Max(240) int etaMinutes,
                              Boolean active,
                              int sortOrder) {

        public DeliveryZone toZone() {
            return new DeliveryZone(code, nameAr.trim(), nameEn.trim(), kind, fee, etaMinutes, active == null || active, sortOrder);
        }
    }

    // ---------- responses ----------

    public record Quote(BigDecimal subtotal, BigDecimal deliveryFee, BigDecimal discount, BigDecimal total, int etaMinutes) {
    }

    public record StoreRef(String id, String name, String nameAr, Double lat, Double lng) {
    }

    public record DriverRef(String name, String phone, Double lat, Double lng) {
    }

    public record ItemView(String productId, String name, String nameAr, BigDecimal unitPrice, int qty, String note) {
        public static ItemView of(OrderItem i) {
            return new ItemView(i.productId(), i.name(), i.nameAr(), i.unitPrice(), i.qty(), i.note());
        }
    }

    public record StepView(OrderStatus status, Instant at) {
    }

    public record OrderView(String code, OrderKind kind, OrderStatus status, StoreRef store, String zone, String phone,
                            String address, String landmark, Double lat, Double lng, String errandDetails,
                            List<ItemView> items, BigDecimal subtotal, BigDecimal discount, BigDecimal deliveryFee,
                            BigDecimal total, BigDecimal cashChangeFor, PaymentMethod paymentMethod,
                            PaymentStatus paymentStatus, String rejectReason, Instant scheduledFor, Instant createdAt,
                            String customerName, DriverRef driver, List<StepView> timeline) {
    }

    public record ZoneView(String code, String nameAr, String nameEn, String kind, BigDecimal fee, int etaMinutes,
                           boolean active) {
        public static ZoneView of(DeliveryZone z) {
            return new ZoneView(z.code(), z.nameAr(), z.nameEn(), z.kind(), z.fee(), z.etaMinutes(), z.active());
        }
    }
}
