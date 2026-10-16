package com.velox.common.error;

import java.util.Map;

/** The one error shape of the API: {"code": "...", "message": "...", "details": {...}}. */
public record ApiError(String code, String message, Map<String, Object> details) {
}
