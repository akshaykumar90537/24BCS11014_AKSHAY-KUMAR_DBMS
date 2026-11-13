package com.resolveit.exception;

import org.springframework.http.HttpStatus;

public class PayloadTooLargeException extends ApiException {
    public PayloadTooLargeException(String message) {
        super(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", message);
    }
}

