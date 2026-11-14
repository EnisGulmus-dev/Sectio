package com.enis.sectio.task;

import com.enis.sectio.common.web.PageResponse;
import com.enis.sectio.project.Project;
import com.enis.sectio.project.ProjectException;
import com.enis.sectio.project.ProjectService;
import com.enis.sectio.project.ProjectTeamService;
import com.enis.sectio.user.User;
import com.enis.sectio.user.UserRepository;
import com.enis.sectio.user.Role;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class TaskService {
    private final TaskRepository tasks;
    private final ProjectService projects;
    private final ProjectTeamService teams;
    private final UserRepository users;

    public TaskService(TaskRepository tasks, ProjectService projects, ProjectTeamService teams, UserRepository users) {
        this.tasks = tasks; this.projects = projects; this.teams = teams; this.users = users;
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> listProject(Long projectId, String email, String query, TaskStatus status,
                                                   TaskPriority priority, Long assigneeId, int page, int size) {
        projects.accessibleEntity(projectId, email);
        return PageResponse.from(tasks.findProjectTasks(projectId, normalize(query), status, priority, assigneeId,
                page(page, size)), TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> mine(String email, String query, TaskStatus status, TaskPriority priority,
                                            int page, int size) {
        return PageResponse.from(tasks.findAssignedTasks(email, normalize(query), status, priority,
                page(page, size)), TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id, String email) { return TaskResponse.from(accessible(id, email)); }

    @Transactional
    public TaskResponse create(Long projectId, String email, List<Long> assigneeIds, List<Long> teamIds,
                               boolean allMembers, String title, String description, TaskPriority priority, Instant dueAt) {
        Project project = projects.manageableEntity(projectId, email);
        active(project);
        return TaskResponse.from(tasks.save(new Task(project, projects.actor(email),
                resolveAssignees(project, assigneeIds, teamIds, allMembers), title, description, priority, dueAt)));
    }

    @Transactional
    public TaskResponse update(Long id, String email, List<Long> assigneeIds, List<Long> teamIds,
                               boolean allMembers, String title, String description, TaskPriority priority, Instant dueAt) {
        Task task = manageable(id, email);
        active(task.getProject());
        task.update(resolveAssignees(task.getProject(), assigneeIds, teamIds, allMembers), title, description, priority, dueAt);
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse status(Long id, String email, TaskStatus status) {
        Task task = accessible(id, email);
        User actor = projects.actor(email);
        boolean managesProject = actor.getRole() == Role.ADMIN || task.getProject().getOwner().getId().equals(actor.getId());
        boolean assigned = task.getAssignees().stream().anyMatch(user -> user.getId().equals(actor.getId()));
        if (!managesProject && !assigned) {
            throw error("TASK_STATUS_DENIED", "Only project managers or assigned users can change task status", HttpStatus.FORBIDDEN);
        }
        active(task.getProject()); task.changeStatus(status); return TaskResponse.from(task);
    }

    @Transactional
    public void delete(Long id, String email) { Task task = manageable(id, email); active(task.getProject()); tasks.delete(task); }

    private Set<User> resolveAssignees(Project project, List<Long> assigneeIds, List<Long> teamIds, boolean allMembers) {
        Set<User> resolved = new LinkedHashSet<>();
        if (allMembers) resolved.addAll(projects.activeMembers(project));
        resolved.addAll(teams.resolveForTask(project, teamIds));
        for (Long id : assigneeIds == null ? List.<Long>of() : new LinkedHashSet<>(assigneeIds)) {
            User user = users.findById(id).orElseThrow(() -> error("USER_NOT_FOUND", "Assignee was not found", HttpStatus.NOT_FOUND));
            if (!user.isActive() || !projects.isMember(project, user)) {
                throw error("INVALID_ASSIGNEE", "Assignees must be active project members", HttpStatus.CONFLICT);
            }
            resolved.add(user);
        }
        if (resolved.isEmpty()) throw error("ASSIGNEE_REQUIRED", "Task must have at least one active project member", HttpStatus.BAD_REQUEST);
        return resolved;
    }

    private Task accessible(Long id, String email) {
        Task task = tasks.findById(id).orElseThrow(() -> error("TASK_NOT_FOUND", "Task was not found", HttpStatus.NOT_FOUND));
        projects.accessibleEntity(task.getProject().getId(), email); return task;
    }
    private Task manageable(Long id, String email) {
        Task task = tasks.findById(id).orElseThrow(() -> error("TASK_NOT_FOUND", "Task was not found", HttpStatus.NOT_FOUND));
        projects.manageableEntity(task.getProject().getId(), email); return task;
    }
    private static void active(Project project) { if (project.isArchived()) throw error("PROJECT_ARCHIVED", "Archived project cannot be changed", HttpStatus.CONFLICT); }
    private static String normalize(String query) { return query == null ? "" : query.trim(); }
    private static Pageable page(int page, int size) { return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("updatedAt").descending()); }
    private static ProjectException error(String code, String message, HttpStatus status) { return new ProjectException(code, message, status); }
}
