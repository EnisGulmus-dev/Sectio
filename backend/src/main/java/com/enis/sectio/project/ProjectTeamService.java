package com.enis.sectio.project;

import com.enis.sectio.user.User;
import com.enis.sectio.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProjectTeamService {
    private final ProjectTeamRepository teams;
    private final ProjectService projects;
    private final UserRepository users;

    public ProjectTeamService(ProjectTeamRepository teams, ProjectService projects, UserRepository users) {
        this.teams = teams;
        this.projects = projects;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<ProjectTeamResponse> list(Long projectId, String email) {
        projects.accessibleEntity(projectId, email);
        return teams.findByProjectIdOrderByNameAsc(projectId).stream().map(ProjectTeamResponse::from).toList();
    }

    @Transactional
    public ProjectTeamResponse create(Long projectId, String email, String name, List<Long> userIds) {
        Project project = projects.manageableEntity(projectId, email);
        projects.requireActive(project);
        String normalizedName = name.trim();
        if (teams.existsByProjectIdAndNameIgnoreCase(projectId, normalizedName)) {
            throw error("TEAM_NAME_ALREADY_EXISTS", "A team with this name already exists", HttpStatus.CONFLICT);
        }
        return ProjectTeamResponse.from(teams.save(new ProjectTeam(project, normalizedName, resolveMembers(project, userIds))));
    }

    @Transactional
    public ProjectTeamResponse update(Long projectId, Long teamId, String email, String name, List<Long> userIds) {
        Project project = projects.manageableEntity(projectId, email);
        projects.requireActive(project);
        ProjectTeam team = find(projectId, teamId);
        String normalizedName = name.trim();
        if (!team.getName().equalsIgnoreCase(normalizedName)
                && teams.existsByProjectIdAndNameIgnoreCase(projectId, normalizedName)) {
            throw error("TEAM_NAME_ALREADY_EXISTS", "A team with this name already exists", HttpStatus.CONFLICT);
        }
        team.update(normalizedName, resolveMembers(project, userIds));
        return ProjectTeamResponse.from(team);
    }

    @Transactional
    public void delete(Long projectId, Long teamId, String email) {
        Project project = projects.manageableEntity(projectId, email);
        projects.requireActive(project);
        teams.delete(find(projectId, teamId));
    }

    @Transactional(readOnly = true)
    public Set<User> resolveForTask(Project project, List<Long> teamIds) {
        Set<User> resolved = new LinkedHashSet<>();
        for (Long teamId : teamIds == null ? List.<Long>of() : teamIds) {
            resolved.addAll(find(project.getId(), teamId).getMembers());
        }
        return resolved;
    }

    private Set<User> resolveMembers(Project project, List<Long> userIds) {
        Set<User> resolved = new LinkedHashSet<>();
        for (Long userId : new LinkedHashSet<>(userIds)) {
            User user = users.findById(userId)
                    .orElseThrow(() -> error("USER_NOT_FOUND", "Team member was not found", HttpStatus.NOT_FOUND));
            if (!user.isActive() || !projects.isMember(project, user)) {
                throw error("INVALID_TEAM_MEMBER", "Team members must be active project members", HttpStatus.CONFLICT);
            }
            resolved.add(user);
        }
        return resolved;
    }

    private ProjectTeam find(Long projectId, Long teamId) {
        return teams.findByIdAndProjectId(teamId, projectId)
                .orElseThrow(() -> error("TEAM_NOT_FOUND", "Project team was not found", HttpStatus.NOT_FOUND));
    }

    private static ProjectException error(String code, String message, HttpStatus status) {
        return new ProjectException(code, message, status);
    }
}
