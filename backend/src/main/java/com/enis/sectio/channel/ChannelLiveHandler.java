package com.enis.sectio.channel;

import com.enis.sectio.user.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChannelLiveHandler extends TextWebSocketHandler {
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ChannelService channels;
    private final UserRepository users;
    public ChannelLiveHandler(ChannelService channels, UserRepository users) { this.channels = channels; this.users = users; }
    @Override public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        WebSocketSession safe = new ConcurrentWebSocketSessionDecorator(session, 5000, 16384);
        if (!authorized(safe)) { safe.close(CloseStatus.POLICY_VIOLATION); return; }
        sessions.put(safe.getId(), safe);
        safe.sendMessage(new TextMessage("{\"type\":\"connected\"}"));
    }
    @Override protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Mutations always go through the CSRF-protected HTTP API.
        session.close(CloseStatus.POLICY_VIOLATION);
    }
    @Override public void afterConnectionClosed(WebSocketSession session, CloseStatus status) { sessions.remove(session.getId()); }
    @Override public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        sessions.remove(session.getId()); if (session.isOpen()) session.close(CloseStatus.SERVER_ERROR);
    }
    @TransactionalEventListener
    public void changed(ChannelLiveEvent event) {
        sessions.values().stream().filter(session -> event.channelId().equals(session.getAttributes().get("channelId"))).forEach(session -> {
            if (!authorized(session)) { close(session); return; }
            try { session.sendMessage(new TextMessage("{\"type\":\"messages_changed\"}")); }
            catch (IOException | RuntimeException ex) { close(session); }
        });
    }
    @Scheduled(fixedDelay = 10000)
    public void revalidate() { sessions.values().forEach(session -> { if (!authorized(session)) close(session); }); }
    private boolean authorized(WebSocketSession session) {
        try {
            if (!session.isOpen()) return false;
            HttpSession http = (HttpSession) session.getAttributes().get("httpSession");
            if (http == null || http.getAttribute("SPRING_SECURITY_CONTEXT") == null) return false;
            Long id = (Long) session.getAttributes().get("userId");
            Long version = (Long) session.getAttributes().get("sessionVersion");
            var user = users.findById(id).orElseThrow();
            if (!user.isActive() || user.getDeletedAt() != null || user.isMustChangePassword() || user.getSessionVersion() != version) return false;
            channels.requireMember((Long) session.getAttributes().get("channelId"), id);
            return true;
        } catch (RuntimeException ex) { return false; }
    }
    private void close(WebSocketSession session) {
        sessions.remove(session.getId());
        try { if (session.isOpen()) session.close(CloseStatus.POLICY_VIOLATION); } catch (IOException ignored) { }
    }
}
