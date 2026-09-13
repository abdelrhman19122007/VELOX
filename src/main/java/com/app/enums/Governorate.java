package com.app.enums;

/**
 * Governorates of Egypt with stable numeric codes, shipping prices (EGP)
 * and estimated delivery days.
 * Core-zone prices match Delivery.calculateBasicPrice: CAIRO=20, GIZA=25,
 * ALEXANDRIA=40, DAMIETTA=60. Delivery fees are computed from
 * {@link #getShippingPrice()} so every governorate is priced.
 */
public enum Governorate {
    CAIRO(1, "القاهرة", 20.0, 1),
    GIZA(2, "الجيزة", 25.0, 1),
    ALEXANDRIA(3, "الإسكندرية", 40.0, 2),
    DAKAHLIA(4, "الدقهلية", 50.0, 2),
    RED_SEA(5, "البحر الأحمر", 70.0, 4),
    BEHEIRA(6, "البحيرة", 50.0, 2),
    FAYOUM(7, "الفيوم", 45.0, 2),
    GHARBIA(8, "الغربية", 50.0, 2),
    ISMAILIA(9, "الإسماعيلية", 55.0, 3),
    MENOFIA(10, "المنوفية", 45.0, 2),
    MINYA(11, "المنيا", 60.0, 3),
    QALYUBIA(12, "القليوبية", 30.0, 1),
    NEW_VALLEY(13, "الوادي الجديد", 80.0, 5),
    SUEZ(14, "السويس", 55.0, 3),
    ASWAN(15, "أسوان", 75.0, 4),
    ASYUT(16, "أسيوط", 65.0, 3),
    BENI_SUEF(17, "بني سويف", 50.0, 2),
    PORT_SAID(18, "بورسعيد", 60.0, 3),
    DAMIETTA(19, "دمياط", 60.0, 3),
    SHARKIA(20, "الشرقية", 45.0, 2),
    SOUTH_SINAI(21, "جنوب سيناء", 75.0, 4),
    KAFR_EL_SHEIKH(22, "كفر الشيخ", 55.0, 3),
    MATROUH(23, "مطروح", 70.0, 4),
    LUXOR(24, "الأقصر", 70.0, 4),
    NORTH_SINAI(25, "شمال سيناء", 75.0, 5),
    QENA(26, "قنا", 70.0, 4),
    SOHAG(27, "سوهاج", 65.0, 3);

    private final int code;
    private final String arabicName;
    private final double shippingPrice;
    private final int deliveryDays;

    Governorate(int code, String arabicName, double shippingPrice, int deliveryDays) {
        this.code = code;
        this.arabicName = arabicName;
        this.shippingPrice = shippingPrice;
        this.deliveryDays = deliveryDays;
    }

    /** Stable numeric code shown to the user (1-27). */
    public int getCode() {
        return code;
    }

    public String getArabicName() {
        return arabicName;
    }

    public double getShippingPrice() {
        return shippingPrice;
    }

    public int getDeliveryDays() {
        return deliveryDays;
    }

    /**
     * Case-insensitive lookup by enum name. Throws IllegalArgumentException if not found.
     */
    public static Governorate fromName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Governorate name cannot be null");
        }
        String normalized = name.trim().toUpperCase().replace(" ", "_");
        for (Governorate g : values()) {
            if (g.name().equals(normalized)) {
                return g;
            }
        }
        throw new IllegalArgumentException("Unknown governorate: " + name);
    }

    /** Lookup by numeric code (1-27). Throws IllegalArgumentException if unknown. */
    public static Governorate fromCode(int code) {
        for (Governorate g : values()) {
            if (g.code == code) {
                return g;
            }
        }
        throw new IllegalArgumentException("Unknown governorate code: " + code + " (valid: 1-27)");
    }

    /**
     * Accepts either a numeric code ("7") or a name ("FAYOUM").
     * Throws IllegalArgumentException when neither matches.
     */
    public static Governorate fromCodeOrName(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Governorate is required (code 1-27 or name).");
        }
        String clean = input.trim();
        try {
            return fromCode(Integer.parseInt(clean));
        } catch (NumberFormatException notACode) {
            return fromName(clean);
        }
    }

    /** One console line per governorate: code | NAME | price | days. */
    public static String menuLine(Governorate g) {
        return String.format(java.util.Locale.US, "%2d | %-13s | %5.0f EGP | ~%d day(s)",
                g.code, g.name(), g.shippingPrice, g.deliveryDays);
    }
}
