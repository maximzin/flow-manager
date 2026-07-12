package com.zinoviev.flowManager.conversion.exception;

import com.zinoviev.flowManager.core.exception.ResourceNotFoundException;

public class ConversionTaskNotFoundException extends ResourceNotFoundException {
    public ConversionTaskNotFoundException(String message) {
        super(message);
    }
}
