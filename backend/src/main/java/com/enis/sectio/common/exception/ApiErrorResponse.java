package com.enis.sectio.common.exception;

import java.util.Map;

public record ApiErrorResponse(
        String code,
        String message,
        Map<String, String> fieldErrors
) {
    public ApiErrorResponse(String code, String message) {
        this(code, message, Map.of());
    }
}
