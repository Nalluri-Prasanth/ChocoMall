package com.chocomall.common.exception;

import com.chocomall.common.api.CommonResult;
import com.chocomall.common.api.ResultCode;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Converts every failure into the {@link CommonResult} envelope with a matching HTTP status. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<CommonResult<Void>> handleApi(ApiException ex) {
        return respond(ex.getErrorCode().getCode(), CommonResult.failed(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<CommonResult<Void>> handleBind(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return respond(400, CommonResult.validateFailed(message.isBlank() ? "Invalid request" : message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<CommonResult<Void>> handleConstraint(ConstraintViolationException ex) {
        return respond(400, CommonResult.validateFailed(ex.getMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, PropertyReferenceException.class})
    public ResponseEntity<CommonResult<Void>> handleMalformed(Exception ex) {
        return respond(400, CommonResult.validateFailed("Malformed request: " + rootMessage(ex)));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<CommonResult<Void>> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return respond(405, CommonResult.failed(ResultCode.FAILED, ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<CommonResult<Void>> handleNoResource(NoResourceFoundException ex) {
        return respond(404, CommonResult.failed(ResultCode.NOT_FOUND));
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    public ResponseEntity<CommonResult<Void>> handleConflict(Exception ex) {
        log.warn("Data conflict: {}", rootMessage(ex));
        return respond(409, CommonResult.failed(ResultCode.CONFLICT,
                "The data was changed by someone else or violates a constraint. Reload and try again."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResult<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return respond(500, CommonResult.failed(ResultCode.FAILED, "An unexpected error occurred"));
    }

    private ResponseEntity<CommonResult<Void>> respond(int status, CommonResult<Void> body) {
        return ResponseEntity.status(HttpStatus.valueOf(status)).body(body);
    }

    private String rootMessage(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t.getMessage();
    }
}
