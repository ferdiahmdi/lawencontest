package com.lawencon.leave_service.dto;

public record AuthLoginResponse(
    String token,
    String tokenType,
    long expiresIn) {
}
