package com.lawencon.leave_service.service;

import com.lawencon.leave_service.domain.Employee;
import com.lawencon.leave_service.domain.Leave;
import com.lawencon.leave_service.domain.LeaveStatus;
import com.lawencon.leave_service.domain.Role;
import com.lawencon.leave_service.dto.LeaveCreateRequest;
import com.lawencon.leave_service.dto.LeaveResponse;
import com.lawencon.leave_service.repository.EmployeeRepository;
import com.lawencon.leave_service.repository.LeaveRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LeaveService {

  private static final EnumSet<LeaveStatus> BLOCKING_STATUSES = EnumSet.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);

  private final LeaveRepository leaveRepository;
  private final EmployeeRepository employeeRepository;

  public LeaveService(LeaveRepository leaveRepository, EmployeeRepository employeeRepository) {
    this.leaveRepository = leaveRepository;
    this.employeeRepository = employeeRepository;
  }

  public LeaveResponse createLeave(String username, LeaveCreateRequest request) {
    Employee employee = findEmployee(username);
    validateDates(request.startDate(), request.endDate());

    boolean overlaps = leaveRepository.findAllByEmployeeIdAndStatusIn(employee.getId(), BLOCKING_STATUSES)
        .stream()
        .anyMatch(existing -> {
          return !existing.getStartDate().isAfter(request.endDate())
              && !existing.getEndDate().isBefore(request.startDate());
        });
    if (overlaps) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Leave overlaps existing request");
    }

    int requestedDays = countDays(request.startDate(), request.endDate());
    int usedDays = usedDaysThisYear(employee.getId(), request.startDate().getYear());
    if (usedDays + requestedDays > employee.getQuota()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Leave quota exceeded");
    }

    Leave leave = new Leave();
    leave.setEmployee(employee);
    leave.setStatus(LeaveStatus.PENDING);
    leave.setStartDate(request.startDate());
    leave.setEndDate(request.endDate());
    leave.setReason(request.reason());

    return toResponse(leaveRepository.save(leave));
  }

  public Page<LeaveResponse> listOwnLeaves(String username, LeaveStatus status, LocalDate startDate, LocalDate endDate,
      Pageable pageable) {
    Employee employee = findEmployee(username);
    UUID employeeId = employee.getId();

    if (status != null && startDate != null && endDate != null) {
      return leaveRepository
          .findAllByEmployeeIdAndStatusAndStartDateGreaterThanEqualAndEndDateLessThanEqual(
              employeeId, status, startDate, endDate, pageable)
          .map(this::toResponse);
    }
    if (status != null) {
      return leaveRepository.findAllByEmployeeIdAndStatus(employeeId, status, pageable)
          .map(this::toResponse);
    }
    if (startDate != null && endDate != null) {
      return leaveRepository
          .findAllByEmployeeIdAndStartDateGreaterThanEqualAndEndDateLessThanEqual(
              employeeId, startDate, endDate, pageable)
          .map(this::toResponse);
    }

    return leaveRepository.findAllByEmployeeId(employeeId, pageable).map(this::toResponse);
  }

  public Page<LeaveResponse> listAllLeaves(UUID employeeId, LeaveStatus status, LocalDate startDate, LocalDate endDate,
      Pageable pageable) {
    if (employeeId != null) {
      Employee employee = employeeRepository.findById(employeeId)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee Not Found"));
      System.out.println("EMPLOYEE ID >>>>>>>>>> " + employee.getId());
      return listOwnLeaves(employee.getUsername(), status, startDate, endDate, pageable);
    }

    if (status != null && startDate != null && endDate != null) {
      return leaveRepository.findAllByStatusAndStartDateGreaterThanEqualAndEndDateLessThanEqual(
          status, startDate, endDate, pageable).map(this::toResponse);
    }
    if (status != null) {
      return leaveRepository.findAllByStatus(status, pageable).map(this::toResponse);
    }
    if (startDate != null && endDate != null) {
      return leaveRepository.findAllByStartDateGreaterThanEqualAndEndDateLessThanEqual(
          startDate, endDate, pageable).map(this::toResponse);
    }

    return leaveRepository.findAll(pageable).map(this::toResponse);
  }

  public LeaveResponse approveLeave(UUID leaveId, String managerUsername) {
    Leave leave = getLeave(leaveId);
    Employee manager = findEmployee(managerUsername);

    if (leave.getStatus() != LeaveStatus.PENDING) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Leave is not pending");
    }

    leave.setStatus(LeaveStatus.APPROVED);
    leave.setApprovedBy(manager);
    return toResponse(leaveRepository.save(leave));
  }

  public LeaveResponse rejectLeave(UUID leaveId, String managerUsername) {
    Leave leave = getLeave(leaveId);
    Employee manager = findEmployee(managerUsername);

    if (leave.getStatus() != LeaveStatus.PENDING) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Leave is not pending");
    }

    leave.setStatus(LeaveStatus.REJECTED);
    leave.setApprovedBy(manager);
    return toResponse(leaveRepository.save(leave));
  }

  private Employee findEmployee(String username) {
    return employeeRepository.findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
  }

  private Leave getLeave(UUID leaveId) {
    return leaveRepository.findById(leaveId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave not found"));
  }

  private void validateDates(LocalDate startDate, LocalDate endDate) {
    if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date range");
    }
    if (startDate.getYear() != endDate.getYear()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leave must be within a single calendar year");
    }
  }

  private int usedDaysThisYear(UUID employeeId, int year) {
    List<Leave> leaves = leaveRepository.findAllByEmployeeIdAndStatusIn(employeeId, BLOCKING_STATUSES);
    return leaves.stream()
        .filter(leave -> leave.getStartDate().getYear() == year && leave.getEndDate().getYear() == year)
        .mapToInt(leave -> countDays(leave.getStartDate(), leave.getEndDate()))
        .sum();
  }

  private int countDays(LocalDate startDate, LocalDate endDate) {
    return Math.toIntExact(ChronoUnit.DAYS.between(startDate, endDate) + 1);
  }

  private LeaveResponse toResponse(Leave leave) {
    UUID approvedById = leave.getApprovedBy() == null ? null : leave.getApprovedBy().getId();
    return new LeaveResponse(
        leave.getId(),
        leave.getEmployee().getId(),
        leave.getStatus(),
        leave.getStartDate(),
        leave.getEndDate(),
        leave.getReason(),
        approvedById,
        leave.getCreatedAt());
  }
}
