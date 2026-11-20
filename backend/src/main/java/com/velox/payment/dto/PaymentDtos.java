package com.velox.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.velox.payment.repository.PromoRepository.Promo;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** What the payment endpoints receive. */
public final class PaymentDtos {

    private PaymentDtos() {
    }

    public record ConfirmRequest(@NotBlank @Pattern(regexp = "^\\d{6}$", message = "6 digits") String code) {
    }

    public record TopUpRequest(@NotNull @DecimalMin("1") @DecimalMax("1000") BigDecimal amount) {
    }

    public record PromoRequest(@NotBlank @Pattern(regexp = "^[A-Z0-9]{3,20}$") String code,
                               @NotBlank @Pattern(regexp = "^(PERCENT|FIXED|FREE_DELIVERY)$") String kind,
                               @NotNull @DecimalMin("0") @DecimalMax("1000") BigDecimal value,
                               @DecimalMin("0") BigDecimal minOrder,
                               @Min(1) Integer maxUses,
                               Instant expiresAt) {

        public Promo toPromo() {
            if ("PERCENT".equals(kind) && value.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw com.velox.common.error.ApiException.badRequest("INVALID_PROMO", "Percent can't be over 100");
            }
            return new Promo(code, kind, value, minOrder == null ? BigDecimal.ZERO : minOrder, null, maxUses, 0, expiresAt);
        }
    }
}
