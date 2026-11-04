package com.chocomall.common.exception;

import com.chocomall.common.api.IErrorCode;
import com.chocomall.common.api.ResultCode;
import java.util.Optional;

/** Fail-fast helpers that keep service code free of if/throw boilerplate. */
public final class Asserts {

    private Asserts() {
    }

    /** 400: the request itself is wrong. */
    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new ApiException(ResultCode.VALIDATE_FAILED, message);
        }
    }

    /** 409: the request is fine but not allowed in the current state. */
    public static void state(boolean condition, String message) {
        if (!condition) {
            throw new ApiException(ResultCode.CONFLICT, message);
        }
    }

    public static ApiException notFound(String what) {
        return new ApiException(ResultCode.NOT_FOUND, what + " not found");
    }

    public static <T> T found(Optional<T> value, String what) {
        return value.orElseThrow(() -> notFound(what));
    }

    public static <T> T found(T value, String what) {
        if (value == null) {
            throw notFound(what);
        }
        return value;
    }

    public static void fail(IErrorCode code, String message) {
        throw new ApiException(code, message);
    }

    public static void fail(String message) {
        throw new ApiException(ResultCode.FAILED, message);
    }
}
