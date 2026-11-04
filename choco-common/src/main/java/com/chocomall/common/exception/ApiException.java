package com.chocomall.common.exception;

import com.chocomall.common.api.IErrorCode;
import com.chocomall.common.api.ResultCode;
import lombok.Getter;

/** A business-rule violation that is reported to the client with the given code. */
@Getter
public class ApiException extends RuntimeException {

    private final IErrorCode errorCode;

    public ApiException(IErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ApiException(IErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ApiException(String message) {
        this(ResultCode.FAILED, message);
    }
}
