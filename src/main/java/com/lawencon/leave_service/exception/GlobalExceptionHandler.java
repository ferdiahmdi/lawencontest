package com.lawencon.leave_service.exception;

import java.util.List;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
    List<ApiFieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
        .map(this::toFieldError)
        .toList();
    ApiError error = new ApiError("VALIDATION_ERROR", "Invalid request", fieldErrors);
    return ResponseEntity.badRequest().body(error);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex) {
    String message = ex.getReason() == null ? "Request failed" : ex.getReason();
    ApiError error = new ApiError("REQUEST_ERROR", message, List.of());
    return ResponseEntity.status(ex.getStatusCode()).body(error);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
    ApiError error = new ApiError("FORBIDDEN", "Access is denied", List.of());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
    String message = ex instanceof BadCredentialsException
        ? "Invalid username or password"
        : "Authentication required";
    ApiError error = new ApiError("UNAUTHORIZED", message, List.of());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    ApiError error = new ApiError("BAD_REQUEST", "Invalid parameter", List.of());
    return ResponseEntity.badRequest().body(error);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex) {
    ApiError error = new ApiError("BAD_REQUEST", "Malformed JSON request", List.of());
    return ResponseEntity.badRequest().body(error);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
    ApiError error = new ApiError("VALIDATION_ERROR", "Invalid request", List.of());
    return ResponseEntity.badRequest().body(error);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
    ApiError error = new ApiError("METHOD_NOT_ALLOWED", "Method not allowed", List.of());
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
    ApiError error = new ApiError("INTERNAL_ERROR", "Unexpected error", List.of());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  private ApiFieldError toFieldError(FieldError error) {
    return new ApiFieldError(error.getField(), error.getDefaultMessage());
  }
}
