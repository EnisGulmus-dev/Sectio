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
    @Query(value = "select distinct t from Task t join t.assignees a where t.project.id=:projectId and (:query='' or lower(t.title) like lower(concat('%',:query,'%'))) and (:status is null or t.status=:status) and (:priority is null or t.priority=:priority) and (:assigneeId is null or a.id=:assigneeId)",
            countQuery = "select count(distinct t.id) from Task t join t.assignees a where t.project.id=:projectId and (:query='' or lower(t.title) like lower(concat('%',:query,'%'))) and (:status is null or t.status=:status) and (:priority is null or t.priority=:priority) and (:assigneeId is null or a.id=:assigneeId)")
    Page<Task> findProjectTasks(@Param("projectId") Long projectId, @Param("query") String query, @Param("status") TaskStatus status,
                                @Param("priority") TaskPriority priority, @Param("assigneeId") Long assigneeId, Pageable pageable);

    @Query(value = "select distinct t from Task t join t.assignees a where a.email=:email and (:query='' or lower(t.title) like lower(concat('%',:query,'%')) or lower(t.project.name) like lower(concat('%',:query,'%'))) and (:status is null or t.status=:status) and (:priority is null or t.priority=:priority)",
            countQuery = "select count(distinct t.id) from Task t join t.assignees a where a.email=:email and (:query='' or lower(t.title) like lower(concat('%',:query,'%')) or lower(t.project.name) like lower(concat('%',:query,'%'))) and (:status is null or t.status=:status) and (:priority is null or t.priority=:priority)")
    Page<Task> findAssignedTasks(@Param("email") String email, @Param("query") String query, @Param("status") TaskStatus status,
                                 @Param("priority") TaskPriority priority, Pageable pageable);

    long countByStatusNotIn(Collection<TaskStatus> statuses); long countByStatus(TaskStatus status);
    long countByDueAtBeforeAndStatusNotIn(Instant due, Collection<TaskStatus> statuses);
    long countDistinctByAssigneesEmailAndStatusNotIn(String email, Collection<TaskStatus> statuses);
    long countDistinctByAssigneesEmailAndStatus(String email, TaskStatus status);
    long countDistinctByAssigneesEmailAndDueAtBeforeAndStatusNotIn(String email, Instant due, Collection<TaskStatus> statuses);
    boolean existsByProjectIdAndAssigneesIdAndStatusNotIn(Long projectId, Long assigneeId, Collection<TaskStatus> statuses);
    long countDistinctByAssigneesId(Long userId); long countByCreatedById(Long userId);
    @Query("select count(distinct t.id) from Task t left join t.assignees a where a.id=:userId or t.createdBy.id=:userId")
    long countByUserReference(@Param("userId") Long userId);
    @Modifying @Query(value="DELETE FROM task_assignees WHERE user_id=:userId", nativeQuery=true)
    void deleteAssigneeReferencesByUserId(@Param("userId") Long userId);
    @Modifying @Query(value="DELETE FROM task_assignees WHERE user_id=:userId AND task_id IN (SELECT id FROM tasks WHERE project_id=:projectId)", nativeQuery=true)
    void deleteAssigneeReferencesByProjectAndUser(@Param("projectId") Long projectId, @Param("userId") Long userId);
    @Modifying void deleteByCreatedById(Long userId);
    @Modifying @Query(value="DELETE FROM tasks t WHERE NOT EXISTS (SELECT 1 FROM task_assignees a WHERE a.task_id=t.id)", nativeQuery=true)
    void deleteTasksWithoutAssignees();
    @Modifying @Query("delete from Task t where t.project.id=:projectId") void deleteByProjectId(@Param("projectId") Long projectId);
}
