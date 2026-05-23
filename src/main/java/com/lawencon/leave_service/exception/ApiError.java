package com.lawencon.leave_service.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
    String code,
    String message,
    List<ApiFieldError> fieldErrors) {
}
