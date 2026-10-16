package com.velox.catalog.service;

/** Lets tests reach the package-private helpers of the catalogue services. */
public final class CatalogTestAccess {

    private CatalogTestAccess() {
    }

    public static String escapeLike(String text) {
        return CatalogService.escapeLike(text);
    }

    public static String imageType(byte[] head) {
        return ImageStorage.imageType(head);
    }
}
