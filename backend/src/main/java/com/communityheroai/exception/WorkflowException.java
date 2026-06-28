package com.communityheroai.exception;

import org.springframework.http.HttpStatus;

public class WorkflowException extends RuntimeException {
    private final HttpStatus status;

    public WorkflowException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
