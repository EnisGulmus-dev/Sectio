package com.enis.sectio.common.exception;

import com.enis.sectio.auth.AuthenticatedUserNotFoundException;
import com.enis.sectio.auth.CurrentPasswordMismatchException;
import com.enis.sectio.auth.PasswordReuseException;
import com.enis.sectio.auth.PasswordPolicyViolationException;
import com.enis.sectio.user.admin.UserManagementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiErrorResponse> authenticationFailed() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse(
                        "INVALID_CREDENTIALS",
                        "Email or password is incorrect"
                ));
    }

    @ExceptionHandler(AuthenticatedUserNotFoundException.class)
    ResponseEntity<ApiErrorResponse> authenticatedUserNotFound() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse(
                        "AUTHENTICATED_USER_NOT_FOUND",
                        "Authenticated user no longer exists"
                ));
    }

    @ExceptionHandler(CurrentPasswordMismatchException.class)
    ResponseEntity<ApiErrorResponse> currentPasswordMismatch(
            CurrentPasswordMismatchException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(
                        "CURRENT_PASSWORD_INCORRECT",
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(PasswordReuseException.class)
    ResponseEntity<ApiErrorResponse> passwordReuse(
            PasswordReuseException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(
                        "PASSWORD_REUSE_NOT_ALLOWED",
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(PasswordPolicyViolationException.class)
    ResponseEntity<ApiErrorResponse> passwordPolicyViolation(
            PasswordPolicyViolationException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(
                        "PASSWORD_POLICY_VIOLATION",
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> validationFailed(
            MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(
                        error.getField(),
                        error.getDefaultMessage()
                )
        );

        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(
                        "VALIDATION_FAILED",
                        "Request validation failed",
                        fieldErrors
                ));
    }

    @ExceptionHandler(UserManagementException.class)
    ResponseEntity<ApiErrorResponse> userManagementFailed(UserManagementException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ApiErrorResponse(exception.getCode(), exception.getMessage()));
    }
}
