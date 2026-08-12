package com.zinoviev.flowManager.core.exception;

public class ApplicationException extends RuntimeException {
    private final String message;

    public ApplicationException(String message) {
        super(message);
        this.message = message;
    }

    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }
}
