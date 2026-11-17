package com.enis.sectio.channel;

import com.enis.sectio.security.SessionUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController @RequestMapping("/api/v1/channels")
public class ChannelController {
    private final ChannelService service;
    public ChannelController(ChannelService service) { this.service = service; }
    private Long userId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof SessionUserDetails details) return details.getUserId();
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    @GetMapping public List<ChannelService.ChannelView> list(Authentication authentication) { return service.list(userId(authentication)); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ChannelService.ChannelView create(@Valid @RequestBody ChannelInput input, Authentication authentication) {
        return service.create(userId(authentication), input.name(), input.description(), input.visibility());
    }
    @GetMapping("/{id}") public ChannelService.ChannelView get(@PathVariable Long id, Authentication authentication) { return service.get(id, userId(authentication)); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) { service.delete(id, userId(authentication)); }
    @PutMapping("/{id}/owner/{memberId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transfer(@PathVariable Long id, @PathVariable Long memberId, Authentication authentication) { service.transferOwnership(id, userId(authentication), memberId); }
    @GetMapping("/{id}/members") public List<ChannelService.MemberView> members(@PathVariable Long id, Authentication authentication) { return service.members(id, userId(authentication)); }
    @GetMapping("/{id}/member-candidates") public List<ChannelService.CandidateView> candidates(@PathVariable Long id, Authentication authentication) { return service.candidates(id, userId(authentication)); }
    @PostMapping("/{id}/join") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void join(@PathVariable Long id, Authentication authentication) { service.join(id, userId(authentication)); }
    @PutMapping("/{id}/members/{memberId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void add(@PathVariable Long id, @PathVariable Long memberId, Authentication authentication) { service.addMember(id, userId(authentication), memberId); }
    @DeleteMapping("/{id}/members/{memberId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long id, @PathVariable Long memberId, Authentication authentication) { service.removeMember(id, userId(authentication), memberId); }
    public record ChannelInput(@NotBlank @Size(max=100) String name, @Size(max=500) String description,
                               @NotBlank @Pattern(regexp="PUBLIC|PRIVATE") String visibility) {}
}
