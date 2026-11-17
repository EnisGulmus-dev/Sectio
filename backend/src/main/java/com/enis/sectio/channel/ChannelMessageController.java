package com.enis.sectio.channel;

import com.enis.sectio.security.SessionUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController @RequestMapping("/api/v1/channels/{channelId}/messages")
public class ChannelMessageController {
    private final ChannelMessageService messages;
    public ChannelMessageController(ChannelMessageService messages) { this.messages = messages; }
    private static Long actor(Authentication auth) {
        if (auth.getPrincipal() instanceof SessionUserDetails principal) return principal.getUserId();
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    @GetMapping public List<ChannelMessageService.MessageView> history(@PathVariable Long channelId, Authentication auth,
            @RequestParam(required = false) Long beforeId, @RequestParam(defaultValue = "50") int limit) {
        return messages.history(channelId, actor(auth), beforeId, limit);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ChannelMessageService.MessageView create(@PathVariable Long channelId, Authentication auth, @Valid @RequestBody MessageInput input) {
        return messages.create(channelId, actor(auth), input.body());
    }
    @PatchMapping("/{messageId}")
    public ChannelMessageService.MessageView edit(@PathVariable Long channelId, @PathVariable Long messageId, Authentication auth, @Valid @RequestBody MessageInput input) {
        return messages.edit(channelId, messageId, actor(auth), input.body());
    }
    @DeleteMapping("/{messageId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long channelId, @PathVariable Long messageId, Authentication auth) { messages.delete(channelId, messageId, actor(auth)); }
    public record MessageInput(@NotBlank @Size(max=4000) String body) {}
}
