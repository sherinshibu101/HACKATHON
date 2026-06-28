package com.communityheroai.exception;

import org.springframework.http.HttpStatus;

public class EmailDeliveryException extends RuntimeException {
    private final HttpStatus status;

    public EmailDeliveryException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
