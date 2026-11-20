package com.velox.operation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** What the store, driver, admin and feedback endpoints receive and return. */
public final class OperationDtos {

    static final String PLAIN_TEXT = "^[^<>]*$";

    private OperationDtos() {
    }

    public record ReasonRequest(@NotBlank @Size(max = 200) @Pattern(regexp = PLAIN_TEXT) String reason) {
    }

    public record DriverStatusRequest(@NotNull Boolean online,
                                      @Pattern(regexp = "^(MOTORBIKE|TUKTUK|BICYCLE|CAR)$") String vehicle) {
    }

    public record LocationRequest(@NotNull @DecimalMin("-90") @DecimalMax("90") Double lat,
                                  @NotNull @DecimalMin("-180") @DecimalMax("180") Double lng) {
    }

    public record ReviewRequest(@NotNull @Min(1) @Max(5) Integer storeRating, @Min(1) @Max(5) Integer driverRating,
                                @Size(max = 1000) @Pattern(regexp = PLAIN_TEXT) String comment) {
    }

    public record ComplaintRequest(@NotBlank @Size(min = 5, max = 1000) @Pattern(regexp = PLAIN_TEXT) String details) {
    }

    /** Closing a complaint, optionally with a one-time discount code for the customer (max 200 EGP). */
    public record CloseComplaintRequest(@Size(max = 500) @Pattern(regexp = PLAIN_TEXT) String note,
                                        @DecimalMin("0") @DecimalMax("200") BigDecimal compensation) {
    }

    public record DriverView(boolean online, String vehicle, Double lat, Double lng) {
    }

    public record Earnings(int deliveredToday, BigDecimal feesToday) {
    }

    public record AdminStats(int ordersToday, int deliveredToday, int cancelledToday, BigDecimal salesToday,
                             BigDecimal commissionToday, int driversOnline, int openComplaints) {
    }

    public record StoreStats(int ordersToday, int deliveredToday, int cancelledToday, BigDecimal salesToday) {
    }

    public record ComplaintCreated(String code) {
    }
}
