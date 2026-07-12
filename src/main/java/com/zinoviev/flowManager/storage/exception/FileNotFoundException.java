package com.zinoviev.flowManager.storage.exception;

import com.zinoviev.flowManager.core.exception.ResourceNotFoundException;

public class FileNotFoundException extends ResourceNotFoundException {
    public FileNotFoundException(String message) {
        super(message);
    }
}
