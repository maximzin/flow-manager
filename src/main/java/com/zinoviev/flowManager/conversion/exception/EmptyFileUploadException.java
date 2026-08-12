package com.zinoviev.flowManager.conversion.exception;

import com.zinoviev.flowManager.core.exception.UploadException;

public class EmptyFileUploadException extends UploadException {
    public EmptyFileUploadException(String message) {
        super(message);
    }
}
