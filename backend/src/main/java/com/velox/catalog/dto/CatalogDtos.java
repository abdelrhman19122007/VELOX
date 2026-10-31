package com.velox.catalog.dto;

import java.math.BigDecimal;
import java.util.List;

import com.velox.catalog.domain.Product;
import com.velox.catalog.domain.ProductSize;
import com.velox.catalog.domain.StoreType;
import com.velox.catalog.repository.StoreRepository.RatedStore;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** What the catalogue endpoints receive and return. */
public final class CatalogDtos {

    private CatalogDtos() {
    }

    /** Text shown to other people: no angle brackets, so it can't carry HTML/script (stored XSS, defence in depth). */
    public static final String PLAIN_TEXT = "^[^<>]*$";
    /** HH:mm, or empty for "no published hours". */
    public static final String TIME = "^(([01]\\d|2[0-3]):[0-5]\\d)?$";
    public static final String SECTION = "^[A-Z_]{2,20}$";

    // ---------- responses ----------

    public record StoreSummary(String id, StoreType type, String cuisine, String name, String nameAr, String tagline,
                               String taglineAr, String photoUrl, String opens, String closes, boolean accepting,
                               boolean openNow, Double rating, int ratingCount, Double lat, Double lng) {

        public static StoreSummary of(RatedStore r, boolean openNow) {
            var s = r.store();
            return new StoreSummary(s.id(), s.type(), s.cuisine(), s.name(), s.nameAr(), s.tagline(), s.taglineAr(),
                    s.photoUrl(), text(s.hours().opens()), text(s.hours().closes()), s.accepting(), openNow, r.rating(),
                    r.ratingCount(), s.lat(), s.lng());
        }
    }

    public record ProductView(String id, String storeId, String name, String nameAr, String description, BigDecimal price,
                              ProductSize size, String section, String photoUrl, boolean available, String descriptionEn) {

        public static ProductView of(Product p) {
            return new ProductView(p.id(), p.storeId(), p.name(), p.nameAr(), p.description(), p.price(), p.size(),
                    p.section(), p.photoUrl(), p.available(), p.descriptionEn());
        }
    }

    /** A menu section with its products, in menu order. */
    public record Section(String code, List<ProductView> products) {
    }

    public record StoreDetail(StoreSummary store, String phone, String address, List<Section> sections) {
    }

    public record SearchResult(List<StoreSummary> stores, List<ProductView> products) {
    }

    public record Created(String id) {
    }

    // ---------- requests ----------

    /** Admin: a new store and the sign-in its staff will use (FR-CAT-07). */
    public record StoreRequest(@NotNull StoreType type,
                               @NotBlank @Pattern(regexp = SECTION) String cuisine,
                               @NotBlank @Size(max = 80) @Pattern(regexp = PLAIN_TEXT) String name,
                               @NotBlank @Size(max = 80) @Pattern(regexp = PLAIN_TEXT) String nameAr,
                               @Size(max = 120) @Pattern(regexp = PLAIN_TEXT) String tagline,
                               @Size(max = 120) @Pattern(regexp = PLAIN_TEXT) String taglineAr,
                               @Pattern(regexp = "^(01[0125]\\d{8}|0\\d{8,9})?$") String phone,
                               @Size(max = 300) @Pattern(regexp = PLAIN_TEXT) String address,
                               @Pattern(regexp = TIME) String opens,
                               @Pattern(regexp = TIME) String closes,
                               @DecimalMin("22") @DecimalMax("32") Double lat,
                               @DecimalMin("24") @DecimalMax("37") Double lng,
                               @DecimalMin("0") @DecimalMax("50") BigDecimal commission,
                               @Size(max = 300) String photoUrl,
                               @NotBlank @Email String accountEmail,
                               @NotBlank @Size(min = 8, max = 100) String accountPassword) {
    }

    /** Admin: edit a store. Only the fields sent are changed. */
    public record StoreUpdateRequest(@Pattern(regexp = SECTION) String cuisine,
                                     @Size(min = 1, max = 80) @Pattern(regexp = PLAIN_TEXT) String name,
                                     @Size(min = 1, max = 80) @Pattern(regexp = PLAIN_TEXT) String nameAr,
                                     @Size(max = 120) @Pattern(regexp = PLAIN_TEXT) String tagline,
                                     @Size(max = 120) @Pattern(regexp = PLAIN_TEXT) String taglineAr,
                                     @Pattern(regexp = "^(01[0125]\\d{8}|0\\d{8,9})?$") String phone,
                                     @Size(max = 300) @Pattern(regexp = PLAIN_TEXT) String address,
                                     @Pattern(regexp = TIME) String opens,
                                     @Pattern(regexp = TIME) String closes,
                                     Boolean accepting,
                                     @DecimalMin("0") @DecimalMax("50") BigDecimal commission,
                                     @Size(max = 300) String photoUrl) {
    }

    public record ProductRequest(@NotBlank @Size(max = 120) @Pattern(regexp = PLAIN_TEXT) String name,
                                 @NotBlank @Size(max = 120) @Pattern(regexp = PLAIN_TEXT) String nameAr,
                                 @Size(max = 500) @Pattern(regexp = PLAIN_TEXT) String description,
                                 @Size(max = 500) @Pattern(regexp = PLAIN_TEXT) String descriptionEn,
                                 @NotNull @DecimalMin("1") @DecimalMax("1000000") @Digits(integer = 7, fraction = 2) BigDecimal price,
                                 ProductSize size,
                                 @NotBlank @Pattern(regexp = SECTION) String section,
                                 @Size(max = 300) String photoUrl) {
    }

    /** Store staff: what they may change on their own products (FR-CAT-06). */
    public record ProductUpdateRequest(Boolean available,
                                       @DecimalMin("1") @DecimalMax("1000000") @Digits(integer = 7, fraction = 2) BigDecimal price) {
    }

    public record AcceptingRequest(@NotNull Boolean accepting) {
    }

    private static String text(java.time.LocalTime t) {
        return t == null ? null : t.toString();
    }
}
