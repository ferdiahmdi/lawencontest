package com.lawencon.leave_service.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record LeaveCreateRequest(
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    String reason) {
}
