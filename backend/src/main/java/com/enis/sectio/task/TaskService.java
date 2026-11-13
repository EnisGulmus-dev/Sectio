package com.enis.sectio.task;

import com.enis.sectio.common.web.PageResponse;
import com.enis.sectio.project.Project;
import com.enis.sectio.project.ProjectException;
import com.enis.sectio.project.ProjectService;
import com.enis.sectio.user.User;
import com.enis.sectio.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class TaskService {
    private final TaskRepository tasks;
    private final ProjectService projects;
    private final UserRepository users;

    public TaskService(TaskRepository tasks, ProjectService projects, UserRepository users) {
        this.tasks = tasks;
        this.projects = projects;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> listProject(Long projectId, String email, String query,
                                                   TaskStatus status, TaskPriority priority,
                                                   Long assigneeId, int page, int size) {
        projects.accessibleEntity(projectId, email);
        return PageResponse.from(tasks.findProjectTasks(projectId, normalize(query), status, priority,
                assigneeId, page(page, size)), TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> mine(String email, String query, TaskStatus status,
                                            TaskPriority priority, int page, int size) {
        return PageResponse.from(tasks.findAssignedTasks(email, normalize(query), status, priority,
                page(page, size)), TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id, String email) { return TaskResponse.from(accessible(id, email)); }

    @Transactional
    public TaskResponse create(Long projectId, String email, Long assigneeId, String title,
                               String description, TaskPriority priority, Instant dueAt) {
        Project project = projects.accessibleEntity(projectId, email);
        active(project);
        User actor = projects.actor(email);
        User assignee = assignee(project, assigneeId);
        return TaskResponse.from(tasks.save(new Task(project, actor, assignee, title, description, priority, dueAt)));
    }

    @Transactional
    public TaskResponse update(Long id, String email, Long assigneeId, String title,
                               String description, TaskPriority priority, Instant dueAt) {
        Task task = accessible(id, email);
        active(task.getProject());
        task.update(assignee(task.getProject(), assigneeId), title, description, priority, dueAt);
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse status(Long id, String email, TaskStatus status) {
        Task task = accessible(id, email);
        active(task.getProject());
        task.changeStatus(status);
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(Long id, String email) {
        Task task = accessible(id, email);
        active(task.getProject());
        tasks.delete(task);
    }

    private Task accessible(Long id, String email) {
        Task task = tasks.findById(id).orElseThrow(() -> error("TASK_NOT_FOUND", "Task was not found", HttpStatus.NOT_FOUND));
        projects.accessibleEntity(task.getProject().getId(), email);
        return task;
    }

    private User assignee(Project project, Long id) {
        if (id == null) return null;
        User user = users.findById(id).orElseThrow(() -> error("USER_NOT_FOUND", "Assignee was not found", HttpStatus.NOT_FOUND));
        if (!user.isActive() || !projects.isMember(project, user)) {
            throw error("INVALID_ASSIGNEE", "Assignee must be an active project member", HttpStatus.CONFLICT);
        }
        return user;
    }

    private static void active(Project project) {
        if (project.isArchived()) throw error("PROJECT_ARCHIVED", "Archived project cannot be changed", HttpStatus.CONFLICT);
    }

    private static String normalize(String query) { return query == null ? "" : query.trim(); }

    private static Pageable page(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("updatedAt").descending());
    }

    private static ProjectException error(String code, String message, HttpStatus status) {
        return new ProjectException(code, message, status);
    }
}
