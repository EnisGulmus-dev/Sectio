package com.enis.sectio.task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EndedTaskAssignmentRepository extends JpaRepository<EndedTaskAssignment, Long> {
    Page<EndedTaskAssignment> findByUserIdOrderByEndedAtDescIdDesc(Long userId, Pageable pageable);
}
