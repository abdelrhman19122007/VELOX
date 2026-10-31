package com.velox.catalog.domain;

import java.math.BigDecimal;

/** Something a store sells ({@code products}). {@code size} decides the delivery vehicle (LARGE → tuk-tuk). */
public record Product(String id, String storeId, String name, String nameAr, String description, BigDecimal price,
                      ProductSize size, String section, String photoUrl, boolean available, String descriptionEn) {
}
