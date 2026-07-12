package com.zinoviev.flowManager.conversion.exception;

import com.zinoviev.flowManager.core.exception.ResourceNotFoundException;

public class FileNotConvertedException extends ResourceNotFoundException {
    public FileNotConvertedException(String message) {
        super(message);
    }
}
