package com.chocomall.common.api;

/** Error codes returned in the {@link CommonResult} envelope. */
public interface IErrorCode {

    int getCode();

    String getMessage();
}
