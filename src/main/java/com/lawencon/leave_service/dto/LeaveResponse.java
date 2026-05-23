package com.lawencon.leave_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lawencon.leave_service.domain.LeaveStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LeaveResponse(
    UUID id,
    UUID employeeId,
    LeaveStatus status,
    LocalDate startDate,
    LocalDate endDate,
    String reason,
    UUID approvedById,
    Instant createdAt) {
}
