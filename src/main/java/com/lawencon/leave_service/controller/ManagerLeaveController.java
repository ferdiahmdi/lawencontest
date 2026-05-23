package com.lawencon.leave_service.controller;

import com.lawencon.leave_service.domain.LeaveStatus;
import com.lawencon.leave_service.dto.LeaveDecisionRequest;
import com.lawencon.leave_service.dto.LeaveResponse;
import com.lawencon.leave_service.service.LeaveService;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/leaves")
public class ManagerLeaveController {

  private final LeaveService leaveService;

  public ManagerLeaveController(LeaveService leaveService) {
    this.leaveService = leaveService;
  }

  @GetMapping
  @PreAuthorize("hasRole('MANAGER')")
  public ResponseEntity<Page<LeaveResponse>> listAllLeaves(
      @RequestParam(required = false) UUID employeeId,
      @RequestParam(required = false) LeaveStatus status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      Pageable pageable) {
    return ResponseEntity.ok(leaveService.listAllLeaves(employeeId, status, from, to, pageable));
  }

  @PostMapping("/{id}/approve")
  @PreAuthorize("hasRole('MANAGER')")
  public ResponseEntity<LeaveResponse> approveLeave(
      @PathVariable UUID id,
      @RequestBody(required = false) LeaveDecisionRequest request,
      Authentication authentication) {
    return ResponseEntity.ok(leaveService.approveLeave(id, authentication.getName()));
  }

  @PostMapping("/{id}/reject")
  @PreAuthorize("hasRole('MANAGER')")
  public ResponseEntity<LeaveResponse> rejectLeave(
      @PathVariable UUID id,
      @RequestBody(required = false) LeaveDecisionRequest request,
      Authentication authentication) {
    return ResponseEntity.ok(leaveService.rejectLeave(id, authentication.getName()));
  }
}
