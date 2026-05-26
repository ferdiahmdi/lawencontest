package com.lawencon.leave_service.repository;

import com.lawencon.leave_service.domain.Leave;
import com.lawencon.leave_service.domain.LeaveStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveRepository extends JpaRepository<Leave, UUID> {
    Page<Leave> findAllByEmployeeId(UUID employeeId, Pageable pageable);

    Page<Leave> findAllByEmployeeIdAndStatus(UUID employeeId, LeaveStatus status, Pageable pageable);

    Page<Leave> findAllByEmployeeIdAndStartDateGreaterThanEqualAndEndDateLessThanEqual(
            UUID employeeId,
            LocalDate from,
            LocalDate to,
            Pageable pageable);

    Page<Leave> findAllByEmployeeIdAndStatusAndStartDateGreaterThanEqualAndEndDateLessThanEqual(
            UUID employeeId,
            LeaveStatus status,
            LocalDate from,
            LocalDate to,
            Pageable pageable);

    Page<Leave> findAllByStatus(LeaveStatus status, Pageable pageable);

    Page<Leave> findAllByStartDateGreaterThanEqualAndEndDateLessThanEqual(
            LocalDate from,
            LocalDate to,
            Pageable pageable);

    Page<Leave> findAllByStatusAndStartDateGreaterThanEqualAndEndDateLessThanEqual(
            LeaveStatus status,
            LocalDate from,
            LocalDate to,
            Pageable pageable);

    List<Leave> findAllByEmployeeIdAndStatusIn(UUID employeeId, Collection<LeaveStatus> statuses);

}
