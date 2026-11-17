package com.enis.sectio.channel;

import com.enis.sectio.security.SessionUserDetails;
import com.enis.sectio.user.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import java.util.Map;

@Configuration @EnableWebSocket @EnableScheduling
public class ChannelLiveConfiguration implements WebSocketConfigurer {
    private final ChannelLiveHandler handler;
    private final ChannelService channels;
    private final UserRepository users;
    public ChannelLiveConfiguration(ChannelLiveHandler handler, ChannelService channels, UserRepository users) {
        this.handler = handler; this.channels = channels; this.users = users;
    }
    @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/api/v1/channels/*/live").addInterceptors(new HandshakeInterceptor() {
            @Override public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler ws, Map<String, Object> attrs) {
                if (!(request instanceof ServletServerHttpRequest servlet) || !(request.getPrincipal() instanceof Authentication auth)
                        || !(auth.getPrincipal() instanceof SessionUserDetails principal)) { response.setStatusCode(HttpStatus.UNAUTHORIZED); return false; }
                HttpSession session = servlet.getServletRequest().getSession(false);
                if (session == null) { response.setStatusCode(HttpStatus.UNAUTHORIZED); return false; }
                try {
                    String[] parts = request.getURI().getPath().split("/");
                    Long channelId = Long.valueOf(parts[parts.length - 2]);
                    var user = users.findById(principal.getUserId()).orElseThrow();
                    if (!user.isActive() || user.getDeletedAt() != null || user.isMustChangePassword() || user.getSessionVersion() != principal.getSessionVersion()) throw new IllegalStateException();
                    channels.requireMember(channelId, principal.getUserId());
                    attrs.put("channelId", channelId); attrs.put("userId", principal.getUserId());
                    attrs.put("sessionVersion", principal.getSessionVersion()); attrs.put("httpSession", session);
                    return true;
                } catch (RuntimeException e) { response.setStatusCode(HttpStatus.FORBIDDEN); return false; }
            }
            @Override public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler ws, Exception exception) {}
        });
    }
}
