package com.app.util;

import java.util.HashSet;
import java.util.Set;

public class PromoCodeManager {
    private static final Set<String> validCodes = new HashSet<>();

    static {
        validCodes.add("VELOX10");
        validCodes.add("FIRSTORDER");
        validCodes.add("FREESHIP");
    }

    public static boolean isValidCode(String code) {
        return code != null && validCodes.contains(code.trim().toUpperCase());
    }
}
