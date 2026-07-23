package com.ssafyhub.backend.global.error;

public record ValidationErrorResponse(
        String field,
        String message
) {
}
