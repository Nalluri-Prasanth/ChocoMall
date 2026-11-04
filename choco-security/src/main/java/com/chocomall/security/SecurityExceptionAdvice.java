package com.chocomall.security;

import com.chocomall.common.api.CommonResult;
import com.chocomall.common.api.ResultCode;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Runs before the catch-all handler of choco-common so security failures keep their 401/403 status. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityExceptionAdvice {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<CommonResult<Void>> denied(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(CommonResult.failed(ResultCode.FORBIDDEN));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<CommonResult<Void>> badCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(401).body(CommonResult.failed(ResultCode.UNAUTHORIZED, "Incorrect username or password"));
    }

    @ExceptionHandler(AccountStatusException.class)
    public ResponseEntity<CommonResult<Void>> accountStatus(AccountStatusException ex) {
        return ResponseEntity.status(401).body(CommonResult.failed(ResultCode.UNAUTHORIZED, "This account is disabled or locked"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<CommonResult<Void>> authentication(AuthenticationException ex) {
        return ResponseEntity.status(401).body(CommonResult.failed(ResultCode.UNAUTHORIZED));
    }
}
