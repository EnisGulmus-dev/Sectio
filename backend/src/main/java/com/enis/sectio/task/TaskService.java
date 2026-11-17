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
    private final TaskHandoffHistoryRepository handoffs;

    public TaskService(TaskRepository tasks, ProjectService projects, ProjectTeamService teams, UserRepository users, TaskHandoffHistoryRepository handoffs) {
        this.tasks = tasks; this.projects = projects; this.teams = teams; this.users = users; this.handoffs = handoffs;
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> listProject(Long projectId, String email, String query, TaskStatus status,
                                                   TaskPriority priority, Long assigneeId, boolean unassigned, int page, int size) {
        return listProject(projectId, email, query, status, priority, assigneeId, unassigned, TaskBucket.ALL, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> listProject(Long projectId, String email, String query, TaskStatus status,
            TaskPriority priority, Long assigneeId, boolean unassigned, TaskBucket bucket, int page, int size) {
        projects.accessibleEntity(projectId, email);
        if (unassigned && assigneeId != null) {
            throw error("INVALID_TASK_FILTER", "Unassigned and assignee filters cannot be combined", HttpStatus.BAD_REQUEST);
        }
        return PageResponse.from(tasks.findProjectTasks(projectId, normalize(query), status, priority, assigneeId,
                unassigned, bucket.statuses(), page(page, size)), TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> mine(String email, String query, TaskStatus status, TaskPriority priority,
                                            int page, int size) {
        return mine(email, query, status, priority, TaskBucket.ALL, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> mine(String email, String query, TaskStatus status, TaskPriority priority,
                                         TaskBucket bucket, int page, int size) {
        return PageResponse.from(tasks.findAssignedTasks(email, normalize(query), status, priority,
                bucket.statuses(), page(page, size)), TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id, String email) { return TaskResponse.from(accessible(id, email)); }

    @Transactional
    public TaskResponse create(Long projectId, String email, List<Long> assigneeIds, List<Long> teamIds,
                               boolean allMembers, String title, String description, TaskPriority priority, Instant dueAt) {
        Project project = projects.manageableEntity(projectId, email);
        active(project);
        var selectedTeams = teams.resolveTeamsForTask(project, teamIds);
        Task task = new Task(project, projects.actor(email),
                resolveAssignees(project, assigneeIds, selectedTeams, allMembers), title, description, priority, dueAt);
        task.setTargetTeams(selectedTeams);
        return TaskResponse.from(tasks.save(task));
    }

    @Transactional
    public TaskResponse update(Long id, String email, List<Long> assigneeIds, List<Long> teamIds,
                               boolean allMembers, String title, String description, TaskPriority priority, Instant dueAt) {
        Task task = manageable(id, email);
        active(task.getProject());
        var selectedTeams = teams.resolveTeamsForTask(task.getProject(), teamIds);
        task.update(resolveAssignees(task.getProject(), assigneeIds, selectedTeams, allMembers), title, description, priority, dueAt);
        task.setTargetTeams(selectedTeams);
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse status(Long id, String email, TaskStatus status) {
        return status(id, email, status, null, null);
    }

    @Transactional
    public TaskResponse status(Long id, String email, TaskStatus status, String note, TaskStatus expectedStatus) {
        Task task = accessible(id, email);
        User actor = projects.actor(email);
        boolean managesProject = actor.getRole() == Role.ADMIN || task.getProject().getOwner().getId().equals(actor.getId());
        boolean assigned = task.getAssignees().stream().anyMatch(user -> user.getId().equals(actor.getId()));
        if (!managesProject && !assigned) {
            throw error("TASK_STATUS_DENIED", "Only project managers or assigned users can change task status", HttpStatus.FORBIDDEN);
        }
        active(task.getProject());
        if (expectedStatus != null && expectedStatus != task.getStatus()) throw error("TASK_STATUS_CONFLICT", "Görev durumu değişti; yenileyip tekrar deneyin.", HttpStatus.CONFLICT);
        if (task.getStatus() == status) throw error("TASK_STATUS_UNCHANGED", "Farklı bir durum seçin.", HttpStatus.BAD_REQUEST);
        task.recordStatus(actor, status, note); return TaskResponse.from(task);
    }

    @Transactional(readOnly = true)
    public List<ProgressResponse> progress(Long id, String email) {
        Task task = accessible(id, email);
        return task.getProgressEvents().stream().map(e -> new ProgressResponse(e.getId(), e.getActor().getName(), e.getOldStatus(), e.getNewStatus(), e.getNote(), e.getOccurredAt())).toList();
    }
    public record ProgressResponse(Long id, String actorName, TaskStatus oldStatus, TaskStatus newStatus, String note, Instant occurredAt) {}

    public record HandoffResponse(Long id, Long sourceTaskId, Long targetTaskId, String sourceTitle, String targetTitle,
                                  String recipientPeople, String recipientTeams, String actorName, Instant handedOffAt) {}
    @Transactional(readOnly = true)
    public List<HandoffResponse> handoffHistory(Long id, String email) {
        accessible(id, email);
        return handoffs.findForTask(id).stream().map(h -> new HandoffResponse(h.getId(), h.getSource() == null ? null : h.getSource().getId(),
                h.getTarget() == null ? null : h.getTarget().getId(), h.getSourceTitle(), h.getTargetTitle(),
                h.getRecipientPeople(), h.getRecipientTeams(), h.getActor().getName(), h.getHandedOffAt())).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> successors(Long id, String email) {
        accessible(id, email);
        return tasks.findByPredecessorIdOrderByCreatedAtAsc(id).stream().map(TaskResponse::from).toList();
    }

    @Transactional
    public TaskResponse handoff(Long id, String email, List<Long> assigneeIds, List<Long> teamIds, boolean allMembers,
                                String title, String description, TaskPriority priority, Instant dueAt) {
        Task source = manageable(id, email); active(source.getProject());
        if (source.getStatus() != TaskStatus.DONE) throw error("TASK_NOT_DONE", "Yalnızca tamamlanmış görev devredilebilir.", HttpStatus.CONFLICT);
        var selectedTeams = teams.resolveTeamsForTask(source.getProject(), teamIds);
        User actor = projects.actor(email);
        Task next = new Task(source.getProject(), actor, resolveAssignees(source.getProject(), assigneeIds, selectedTeams, allMembers), title, description, priority, dueAt);
        next.setTargetTeams(selectedTeams); next.setPredecessor(source); source.markHandoff();
        tasks.save(next);
        handoffs.save(new TaskHandoffHistory(source, next, actor));
        return TaskResponse.from(next);
    }

    @Transactional
    public void delete(Long id, String email) { Task task = manageable(id, email); active(task.getProject()); tasks.delete(task); }

    private Set<User> resolveAssignees(Project project, List<Long> assigneeIds,
            Set<com.enis.sectio.project.ProjectTeam> selectedTeams, boolean allMembers) {
        Set<User> resolved = new LinkedHashSet<>();
        if (allMembers) resolved.addAll(projects.activeMembers(project));
        selectedTeams.stream().flatMap(team -> team.getMembers().stream())
                .filter(user -> user.isActive() && user.getDeletedAt() == null && projects.isMember(project, user))
                .forEach(resolved::add);
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
