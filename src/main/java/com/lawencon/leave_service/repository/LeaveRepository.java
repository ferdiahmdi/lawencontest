package com.lawencon.leave_service.repository;

import com.lawencon.leave_service.domain.Leave;
import com.lawencon.leave_service.domain.LeaveStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveRepository extends JpaRepository<Leave, UUID> {
    Page<Leave> findAllByEmployeeId(UUID employeeId, Pageable pageable);

    boolean existsByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            UUID employeeId,
            Collection<LeaveStatus> statuses,
            LocalDate endDate,
            LocalDate startDate);
}
