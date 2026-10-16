package com.velox.order.domain;

import java.math.BigDecimal;

/** A delivery area of Samannoud or a nearby village, with its fixed fee and usual delivery time. */
public record DeliveryZone(String code, String nameAr, String nameEn, String kind, BigDecimal fee, int etaMinutes,
                           boolean active, int sortOrder) {
}
