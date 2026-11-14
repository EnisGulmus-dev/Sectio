package com.enis.sectio.project;

import com.enis.sectio.common.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/projects")
public class ProjectController {
    private final ProjectService service;
    private final ProjectTeamService teamService;
    public ProjectController(ProjectService service, ProjectTeamService teamService) { this.service = service; this.teamService = teamService; }
    @GetMapping PageResponse<ProjectResponse> list(Authentication a, @RequestParam(defaultValue="") String query, @RequestParam(required=false) Boolean archived, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) { return service.list(a.getName(), query, archived, page, size); }
    @GetMapping("/{id}") ProjectResponse get(@PathVariable Long id, Authentication a) { return service.get(id, a.getName()); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) ProjectResponse create(@Valid @RequestBody ProjectInput r, Authentication a) { return service.create(a.getName(), r.name(), r.description()); }
    @PatchMapping("/{id}") ProjectResponse update(@PathVariable Long id, @Valid @RequestBody ProjectInput r, Authentication a) { return service.update(id, a.getName(), r.name(), r.description()); }
    @PostMapping("/{id}/archive") ProjectResponse archive(@PathVariable Long id, Authentication a) { return service.archive(id, a.getName()); }
    @PostMapping("/{id}/restore") ProjectResponse restore(@PathVariable Long id, Authentication a) { return service.restore(id, a.getName()); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@PathVariable Long id, Authentication a) { service.delete(id, a.getName()); }
    @GetMapping("/{id}/members") List<ProjectMemberResponse> members(@PathVariable Long id, Authentication a) { return service.listMembers(id, a.getName()); }
    @GetMapping("/{id}/member-candidates") List<ProjectMemberCandidateResponse> memberCandidates(@PathVariable Long id, Authentication a) { return service.memberCandidates(id, a.getName()); }
    @PostMapping("/{id}/members") @ResponseStatus(HttpStatus.CREATED) List<ProjectMemberResponse> addMembers(@PathVariable Long id, @Valid @RequestBody MemberInput r, Authentication a) { return service.addMembers(id, r.userIds(), a.getName()); }
    @DeleteMapping("/{id}/members/{userId}") @ResponseStatus(HttpStatus.NO_CONTENT) void removeMember(@PathVariable Long id, @PathVariable Long userId, Authentication a) { service.removeMember(id, userId, a.getName()); }
    @GetMapping("/{id}/teams") List<ProjectTeamResponse> teams(@PathVariable Long id, Authentication a) { return teamService.list(id, a.getName()); }
    @PostMapping("/{id}/teams") @ResponseStatus(HttpStatus.CREATED) ProjectTeamResponse createTeam(@PathVariable Long id, @Valid @RequestBody TeamInput r, Authentication a) { return teamService.create(id, a.getName(), r.name(), r.userIds()); }
    @PatchMapping("/{id}/teams/{teamId}") ProjectTeamResponse updateTeam(@PathVariable Long id, @PathVariable Long teamId, @Valid @RequestBody TeamInput r, Authentication a) { return teamService.update(id, teamId, a.getName(), r.name(), r.userIds()); }
    @DeleteMapping("/{id}/teams/{teamId}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteTeam(@PathVariable Long id, @PathVariable Long teamId, Authentication a) { teamService.delete(id, teamId, a.getName()); }
    public record ProjectInput(@NotBlank @Size(max=150) String name, @Size(max=1000) String description) {}
    public record MemberInput(@NotEmpty @Size(max=50) List<@NotNull @Positive Long> userIds) {}
    public record TeamInput(@NotBlank @Size(max=100) String name, @NotEmpty @Size(max=50) List<@NotNull @Positive Long> userIds) {}
}
