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
import org.springframework.data.jpa.domain.Specification;
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
    enforceSameYear(request.startDate(), request.endDate());

    boolean overlaps = leaveRepository.findAllByEmployeeIdAndStatusIn(employee.getId(), BLOCKING_STATUSES)
        .stream()
        .anyMatch(existing -> overlaps(existing.getStartDate(), existing.getEndDate(),
            request.startDate(), request.endDate()));
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

  public Page<LeaveResponse> listOwnLeaves(String username, LeaveStatus status, LocalDate from, LocalDate to,
      Pageable pageable) {
    Employee employee = findEmployee(username);
    Specification<Leave> spec = baseFilter(employee.getId(), status, from, to);
    return leaveRepository.findAll(spec, pageable).map(this::toResponse);
  }

  public Page<LeaveResponse> listAllLeaves(UUID employeeId, LeaveStatus status, LocalDate from, LocalDate to,
      Pageable pageable) {
    Specification<Leave> spec = baseFilter(employeeId, status, from, to);
    return leaveRepository.findAll(spec, pageable).map(this::toResponse);
  }

  public LeaveResponse approveLeave(UUID leaveId, String managerUsername) {
    Leave leave = getLeave(leaveId);
    Employee manager = findEmployee(managerUsername);
    ensureManager(manager);

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
    ensureManager(manager);

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

  private void ensureManager(Employee manager) {
    if (manager.getRole() != Role.MANAGER) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Manager role required");
    }
  }

  private void validateDates(LocalDate startDate, LocalDate endDate) {
    if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date range");
    }
  }

  private void enforceSameYear(LocalDate startDate, LocalDate endDate) {
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

  private boolean overlaps(LocalDate startA, LocalDate endA, LocalDate startB, LocalDate endB) {
    return !startA.isAfter(endB) && !endA.isBefore(startB);
  }

  private Specification<Leave> baseFilter(UUID employeeId, LeaveStatus status, LocalDate from, LocalDate to) {
    Specification<Leave> spec = (root, query, cb) -> cb.conjunction();

    if (employeeId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("employee").get("id"), employeeId));
    }
    if (status != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
    }
    if (from != null) {
      spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startDate"), from));
    }
    if (to != null) {
      spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("endDate"), to));
    }

    return spec;
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
