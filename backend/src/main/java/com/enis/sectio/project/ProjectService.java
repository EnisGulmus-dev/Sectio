package com.enis.sectio.project;

import com.enis.sectio.common.web.PageResponse;
import com.enis.sectio.user.Role;
import com.enis.sectio.user.User;
import com.enis.sectio.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProjectService {
    private final ProjectRepository projects; private final ProjectMemberRepository members; private final UserRepository users;
    public ProjectService(ProjectRepository projects, ProjectMemberRepository members, UserRepository users) { this.projects = projects; this.members = members; this.users = users; }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> list(String email, String query, Boolean archived, int page, int size) {
        User actor = actor(email); var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("updatedAt").descending());
        return PageResponse.from(projects.findAccessible(actor.getEmail(), actor.getRole() == Role.ADMIN, archived, query == null ? "" : query.trim(), pageable), ProjectResponse::from);
    }
    @Transactional(readOnly = true) public ProjectResponse get(Long id, String email) { return ProjectResponse.from(accessible(id, actor(email))); }
    @Transactional
    public ProjectResponse create(String email, String name, String description) {
        User owner = actor(email); Project p = projects.save(new Project(owner, name, description)); members.save(new ProjectMember(p, owner)); return ProjectResponse.from(p);
    }
    @Transactional
    public ProjectResponse update(Long id, String email, String name, String description) { User actor = actor(email); Project p = manageable(id, actor); active(p); p.update(name, description); return ProjectResponse.from(p); }
    @Transactional
    public ProjectResponse archive(Long id, String email) { Project p = manageable(id, actor(email)); active(p); p.archive(); return ProjectResponse.from(p); }
    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> listMembers(Long id, String email) { Project p = accessible(id, actor(email)); return members.findByProjectIdOrderByUserNameAsc(p.getId()).stream().map(ProjectMemberResponse::from).toList(); }
    @Transactional(readOnly = true)
    public List<ProjectMemberCandidateResponse> memberCandidates(Long id, String email) {
        Project p = manageable(id, actor(email));
        active(p);
        return users.findByActiveTrueOrderByNameAsc().stream()
                .filter(user -> !members.existsByProjectIdAndUserId(id, user.getId()))
                .map(ProjectMemberCandidateResponse::from)
                .toList();
    }
    @Transactional
    public ProjectMemberResponse addMember(Long id, Long userId, String email) {
        Project p = manageable(id, actor(email)); active(p); User user = users.findById(userId).orElseThrow(() -> error("USER_NOT_FOUND", "User was not found", HttpStatus.NOT_FOUND));
        if (!user.isActive()) throw error("USER_INACTIVE", "Inactive user cannot join a project", HttpStatus.CONFLICT);
        if (members.existsByProjectIdAndUserId(id, userId)) throw error("MEMBER_ALREADY_EXISTS", "User is already a project member", HttpStatus.CONFLICT);
        return ProjectMemberResponse.from(members.save(new ProjectMember(p, user)));
    }
    @Transactional
    public void removeMember(Long id, Long userId, String email) {
        Project p = manageable(id, actor(email)); active(p);
        if (p.getOwner().getId().equals(userId)) throw error("OWNER_REMOVAL_NOT_ALLOWED", "Project owner cannot be removed", HttpStatus.CONFLICT);
        ProjectMember member = members.findByProjectIdAndUserId(id, userId).orElseThrow(() -> error("MEMBER_NOT_FOUND", "Project member was not found", HttpStatus.NOT_FOUND)); members.delete(member);
    }
    public Project accessibleEntity(Long id, String email) { return accessible(id, actor(email)); }
    public boolean isMember(Project p, User u) { return p.getOwner().getId().equals(u.getId()) || members.existsByProjectIdAndUserId(p.getId(), u.getId()); }
    public User actor(String email) { return users.findByEmail(email).orElseThrow(() -> error("USER_NOT_FOUND", "Authenticated user was not found", HttpStatus.UNAUTHORIZED)); }
    private Project accessible(Long id, User actor) { Project p = find(id); if (actor.getRole() != Role.ADMIN && !isMember(p, actor)) throw error("PROJECT_ACCESS_DENIED", "Project is not accessible", HttpStatus.FORBIDDEN); return p; }
    private Project manageable(Long id, User actor) { Project p = find(id); if (actor.getRole() != Role.ADMIN && !p.getOwner().getId().equals(actor.getId())) throw error("PROJECT_MANAGEMENT_DENIED", "Only an admin or project owner can manage this project", HttpStatus.FORBIDDEN); return p; }
    private Project find(Long id) { return projects.findById(id).orElseThrow(() -> error("PROJECT_NOT_FOUND", "Project was not found", HttpStatus.NOT_FOUND)); }
    private static void active(Project p) { if (p.isArchived()) throw error("PROJECT_ARCHIVED", "Archived project cannot be changed", HttpStatus.CONFLICT); }
    private static ProjectException error(String code, String message, HttpStatus status) { return new ProjectException(code, message, status); }
}
