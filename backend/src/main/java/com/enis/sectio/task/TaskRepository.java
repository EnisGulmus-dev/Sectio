package com.enis.sectio.task;
import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface TaskRepository extends JpaRepository<Task,Long>{
    Page<Task> findByProjectId(Long projectId,Pageable p);
    Page<Task> findByAssigneeEmail(String email,Pageable p);
    long countByStatusNot(TaskStatus s);
    long countByStatus(TaskStatus s);
    long countByDueAtBeforeAndStatusNot(java.time.Instant due,TaskStatus s);
    long countByAssigneeEmailAndStatusNot(String email,TaskStatus s);
    long countByAssigneeEmailAndStatus(String email,TaskStatus s);
    long countByAssigneeEmailAndDueAtBeforeAndStatusNot(String email,java.time.Instant due,TaskStatus s);
}
