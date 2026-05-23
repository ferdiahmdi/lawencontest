package com.lawencon.leave_service.controller;

import com.lawencon.leave_service.domain.LeaveStatus;
import com.lawencon.leave_service.dto.LeaveCreateRequest;
import com.lawencon.leave_service.dto.LeaveResponse;
import com.lawencon.leave_service.service.LeaveService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

  private final LeaveService leaveService;

  public LeaveController(LeaveService leaveService) {
    this.leaveService = leaveService;
  }

  @PostMapping
  public LeaveResponse createLeave(@Valid @RequestBody LeaveCreateRequest request, Authentication authentication) {
    return leaveService.createLeave(authentication.getName(), request);
  }

  @GetMapping
  public Page<LeaveResponse> listOwnLeaves(
      Authentication authentication,
      @RequestParam(required = false) LeaveStatus status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      Pageable pageable) {
    return leaveService.listOwnLeaves(authentication.getName(), status, from, to, pageable);
  }
}
