package com.enis.sectio.organization;

import com.enis.sectio.security.SessionUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController @RequestMapping("/api/v1/organization")
public class OrganizationController {
    private final OrganizationService service;
    public OrganizationController(OrganizationService service) { this.service = service; }
    private static Long actor(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof SessionUserDetails principal) return principal.getUserId();
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    @GetMapping("/teams") public List<OrganizationService.Team> teams(Authentication auth) { return service.teams(actor(auth)); }
    @GetMapping("/managers") public List<OrganizationService.Person> managers(Authentication auth) { return service.managers(actor(auth)); }
    @GetMapping("/reports") public List<OrganizationService.Person> reports(Authentication auth) { return service.reports(actor(auth)); }
    @PostMapping("/teams") @ResponseStatus(HttpStatus.CREATED)
    public OrganizationService.Team createTeam(Authentication auth, @Valid @RequestBody TeamInput input) { return service.createTeam(actor(auth), input.name(), input.userIds()); }
    @PatchMapping("/teams/{id}") public OrganizationService.Team renameTeam(Authentication auth, @PathVariable Long id, @Valid @RequestBody TeamInput input) { return service.renameTeam(actor(auth), id, input.name()); }
    @DeleteMapping("/teams/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeam(Authentication auth, @PathVariable Long id) { service.deleteTeam(actor(auth), id); }
    @GetMapping("/users/{id}") public OrganizationService.Relationship get(Authentication auth, @PathVariable Long id) { return service.relationship(actor(auth), id); }
    @PutMapping("/users/{id}") public OrganizationService.Relationship assign(Authentication auth, @PathVariable Long id, @Valid @RequestBody RelationshipInput input) {
        return service.assign(actor(auth), id, input.managerId(), input.teamIds());
    }
    public record TeamInput(@NotBlank @Size(max=100) String name, @Size(max=100) List<@NotNull @Positive Long> userIds) {}
    public record RelationshipInput(@Positive Long managerId, @NotNull @Size(max=50) List<@NotNull @Positive Long> teamIds) {}
}
