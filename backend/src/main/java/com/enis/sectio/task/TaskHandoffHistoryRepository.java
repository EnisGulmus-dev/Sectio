package com.enis.sectio.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TaskHandoffHistoryRepository extends JpaRepository<TaskHandoffHistory, Long> {
    @Query("select h from TaskHandoffHistory h where h.source.id = :taskId or h.target.id = :taskId order by h.handedOffAt desc, h.id desc")
    List<TaskHandoffHistory> findForTask(@Param("taskId") Long taskId);
}
