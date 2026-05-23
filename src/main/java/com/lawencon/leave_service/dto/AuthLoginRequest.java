package com.lawencon.leave_service.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthLoginRequest(
    @NotBlank String username,
    @NotBlank String password) {
}
