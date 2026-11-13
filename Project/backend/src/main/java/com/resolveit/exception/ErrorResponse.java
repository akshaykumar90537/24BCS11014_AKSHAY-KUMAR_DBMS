package com.resolveit.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        boolean success,
        Instant timestamp,
        int status,
        String errorCode,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    public static ErrorResponse of(int status, String errorCode, String message, String path, Map<String, String> fieldErrors) {
        return new ErrorResponse(false, Instant.now(), status, errorCode, message, path, fieldErrors);
    }
}

