package com.velox.catalog.domain;

/**
 * Kinds of shop in Samannoud. The letters build readable ids: stores R1, G1, P1…, their products F1, Q1, M1…
 * (Factory idea: the type decides how ids are made — see {@link #storePrefix()} / {@link #productPrefix()}).
 */
public enum StoreType {
    FOOD("R", "F"),
    GROCERY("G", "Q"),
    PHARMACY("P", "M"),
    CLOTHES("S", "C"),
    TECH("T", "E"),
    OTHER("O", "X");

    private final String storePrefix;
    private final String productPrefix;

    StoreType(String storePrefix, String productPrefix) {
        this.storePrefix = storePrefix;
        this.productPrefix = productPrefix;
    }

    public String storePrefix() {
        return storePrefix;
    }

    public String productPrefix() {
        return productPrefix;
    }
}
