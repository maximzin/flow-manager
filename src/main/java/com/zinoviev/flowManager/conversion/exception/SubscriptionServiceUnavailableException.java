package com.zinoviev.flowManager.conversion.exception;

import com.zinoviev.flowManager.core.exception.ServiceUnavaiableException;

public class SubscriptionServiceUnavailableException extends ServiceUnavaiableException {
    public SubscriptionServiceUnavailableException(String message) {
        super(message);
    }
}
