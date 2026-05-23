package com.lawencon.leave_service.repository;

import com.lawencon.leave_service.domain.Leave;
import com.lawencon.leave_service.domain.LeaveStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LeaveRepository extends JpaRepository<Leave, UUID>, JpaSpecificationExecutor<Leave> {
    Page<Leave> findAllByEmployeeId(UUID employeeId, Pageable pageable);

    List<Leave> findAllByEmployeeIdAndStatusIn(UUID employeeId, Collection<LeaveStatus> statuses);

}
