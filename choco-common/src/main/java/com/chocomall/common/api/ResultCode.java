package com.chocomall.common.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** The numeric code mirrors the HTTP status that accompanies the response. */
@Getter
@AllArgsConstructor
public enum ResultCode implements IErrorCode {

    SUCCESS(200, "Operation successful"),
    VALIDATE_FAILED(400, "Invalid request parameters"),
    PAYMENT_FAILED(402, "The payment was not completed"),
    UNAUTHORIZED(401, "Not logged in or the token has expired"),
    FORBIDDEN(403, "You do not have permission to perform this action"),
    NOT_FOUND(404, "Resource not found"),
    CONFLICT(409, "The request conflicts with the current state"),
    TOO_MANY_REQUESTS(429, "Too many attempts, please try again later"),
    FAILED(500, "Operation failed");

    private final int code;
    private final String message;
}
