package com.app.enums;

/**
 * Governorates of Egypt with shipping prices (EGP) and estimated delivery days.
 * Prices are aligned with Delivery.calculateBasicPrice for the core zones:
 * CAIRO=20, GIZA=25, ALEXANDRIA=40, DAMIETTA=60.
 */
public enum Governorate {
    CAIRO("القاهرة", 20.0, 1),
    GIZA("الجيزة", 25.0, 1),
    ALEXANDRIA("الإسكندرية", 40.0, 2),
    DAKAHLIA("الدقهلية", 50.0, 2),
    RED_SEA("البحر الأحمر", 70.0, 4),
    BEHEIRA("البحيرة", 50.0, 2),
    FAYOUM("الفيوم", 45.0, 2),
    GHARBIA("الغربية", 50.0, 2),
    ISMAILIA("الإسماعيلية", 55.0, 3),
    MENOFIA("المنوفية", 45.0, 2),
    MINYA("المنيا", 60.0, 3),
    QALYUBIA("القليوبية", 30.0, 1),
    NEW_VALLEY("الوادي الجديد", 80.0, 5),
    SUEZ("السويس", 55.0, 3),
    ASWAN("أسوان", 75.0, 4),
    ASYUT("أسيوط", 65.0, 3),
    BENI_SUEF("بني سويف", 50.0, 2),
    PORT_SAID("بورسعيد", 60.0, 3),
    DAMIETTA("دمياط", 60.0, 3),
    SHARKIA("الشرقية", 45.0, 2),
    SOUTH_SINAI("جنوب سيناء", 75.0, 4),
    KAFR_EL_SHEIKH("كفر الشيخ", 55.0, 3),
    MATROUH("مطروح", 70.0, 4),
    LUXOR("الأقصر", 70.0, 4),
    NORTH_SINAI("شمال سيناء", 75.0, 5),
    QENA("قنا", 70.0, 4),
    SOHAG("سوهاج", 65.0, 3);

    private final String arabicName;
    private final double shippingPrice;
    private final int deliveryDays;

    Governorate(String arabicName, double shippingPrice, int deliveryDays) {
        this.arabicName = arabicName;
        this.shippingPrice = shippingPrice;
        this.deliveryDays = deliveryDays;
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
}
