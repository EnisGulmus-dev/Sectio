package com.enis.sectio.task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;

public interface TaskRepository extends JpaRepository<Task, Long> {
    @Query("""
            select t from Task t
            where t.project.id = :projectId
              and (:query = '' or lower(t.title) like lower(concat('%', :query, '%')))
              and (:status is null or t.status = :status)
              and (:priority is null or t.priority = :priority)
              and (:assigneeId is null or t.assignee.id = :assigneeId)
            """)
    Page<Task> findProjectTasks(@Param("projectId") Long projectId,
                                @Param("query") String query,
                                @Param("status") TaskStatus status,
                                @Param("priority") TaskPriority priority,
                                @Param("assigneeId") Long assigneeId,
                                Pageable pageable);

    @Query("""
            select t from Task t
            where t.assignee.email = :email
              and (:query = '' or lower(t.title) like lower(concat('%', :query, '%'))
                   or lower(t.project.name) like lower(concat('%', :query, '%')))
              and (:status is null or t.status = :status)
              and (:priority is null or t.priority = :priority)
            """)
    Page<Task> findAssignedTasks(@Param("email") String email,
                                 @Param("query") String query,
                                 @Param("status") TaskStatus status,
                                 @Param("priority") TaskPriority priority,
                                 Pageable pageable);

    long countByStatusNotIn(Collection<TaskStatus> statuses);
    long countByStatus(TaskStatus status);
    long countByDueAtBeforeAndStatusNotIn(Instant due, Collection<TaskStatus> statuses);
    long countByAssigneeEmailAndStatusNotIn(String email, Collection<TaskStatus> statuses);
    long countByAssigneeEmailAndStatus(String email, TaskStatus status);
    long countByAssigneeEmailAndDueAtBeforeAndStatusNotIn(String email, Instant due, Collection<TaskStatus> statuses);
    boolean existsByProjectIdAndAssigneeIdAndStatusNotIn(Long projectId, Long assigneeId, Collection<TaskStatus> statuses);
    boolean existsByCreatedById(Long userId);
    boolean existsByAssigneeId(Long userId);
    @Modifying
    @Query("delete from Task task where task.project.id = :projectId")
    void deleteByProjectId(@Param("projectId") Long projectId);
}
