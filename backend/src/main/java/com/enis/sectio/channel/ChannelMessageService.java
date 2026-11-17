package com.enis.sectio.channel;

import com.enis.sectio.project.ProjectException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
public class ChannelMessageService {
    private final JdbcTemplate jdbc;
    private final ChannelService channels;
    private final com.enis.sectio.user.UserRepository users;
    private final org.springframework.context.ApplicationEventPublisher events;
    public ChannelMessageService(JdbcTemplate jdbc, ChannelService channels, com.enis.sectio.user.UserRepository users, org.springframework.context.ApplicationEventPublisher events) { this.jdbc = jdbc; this.channels = channels; this.users = users; this.events = events; }

    public record MessageView(Long id, Long channelId, Long authorId, String authorName, String displayName,
                              String avatarId, boolean hasPhoto, String body, Instant createdAt, Instant editedAt, boolean deleted,
                              String eventType, String subjectName) {}
    private static final String SELECT = """
        SELECT m.id,m.channel_id,m.author_id,u.name author_name,p.display_name,p.avatar_id,
               (p.photo_key IS NOT NULL AND u.active=true AND u.deleted_at IS NULL) has_photo,
               CASE WHEN m.deleted_at IS NULL THEN m.body ELSE NULL END body,
               m.created_at,m.edited_at,(m.deleted_at IS NOT NULL) deleted,m.event_type,subject.name subject_name
        FROM channel_messages m JOIN users u ON u.id=m.author_id LEFT JOIN user_profiles p ON p.user_id=u.id
        LEFT JOIN users subject ON subject.id=m.subject_user_id
        """;
    private static final org.springframework.jdbc.core.RowMapper<MessageView> MAPPER = (rs, n) -> new MessageView(
        rs.getLong("id"), rs.getLong("channel_id"), rs.getLong("author_id"), rs.getString("author_name"),
        rs.getString("display_name"), rs.getString("avatar_id"), rs.getBoolean("has_photo"), rs.getString("body"),
        rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("edited_at") == null ? null : rs.getTimestamp("edited_at").toInstant(), rs.getBoolean("deleted"),
        rs.getString("event_type"), rs.getString("subject_name"));

    @Transactional(readOnly = true)
    public List<MessageView> history(Long channelId, Long actorId, Long beforeId, int limit) {
        channels.requireMember(channelId, actorId);
        int safeLimit = Math.min(100, Math.max(1, limit));
        if (beforeId == null) return jdbc.query(SELECT + " WHERE m.channel_id=? ORDER BY m.id DESC LIMIT ?", MAPPER, channelId, safeLimit);
        return jdbc.query(SELECT + " WHERE m.channel_id=? AND m.id < ? ORDER BY m.id DESC LIMIT ?", MAPPER, channelId, beforeId, safeLimit);
    }

    @Transactional
    public MessageView create(Long channelId, Long actorId, String body) {
        requireMembershipLock(channelId, actorId);
        String normalized = validate(body);
        Long id = jdbc.queryForObject("INSERT INTO channel_messages(channel_id,author_id,body) VALUES (?,?,?) RETURNING id",
                Long.class, channelId, actorId, normalized);
        events.publishEvent(new ChannelLiveEvent(channelId));
        return getMessage(channelId, id);
    }

    @Transactional
    public MessageView edit(Long channelId, Long messageId, Long actorId, String body) {
        requireMembershipLock(channelId, actorId);
        String normalized = validate(body);
        int changed = jdbc.update("UPDATE channel_messages SET body=?,edited_at=CURRENT_TIMESTAMP WHERE channel_id=? AND id=? AND author_id=? AND deleted_at IS NULL AND event_type IS NULL",
                normalized, channelId, messageId, actorId);
        if (changed == 0) throw error("MESSAGE_NOT_EDITABLE", "Mesaj bulunamadı veya düzenleme yetkiniz yok.", HttpStatus.NOT_FOUND);
        events.publishEvent(new ChannelLiveEvent(channelId));
        return getMessage(channelId, messageId);
    }

    @Transactional
    public void delete(Long channelId, Long messageId, Long actorId) {
        requireMembershipLock(channelId, actorId);
        int changed = jdbc.update("UPDATE channel_messages SET body='',deleted_at=CURRENT_TIMESTAMP WHERE channel_id=? AND id=? AND author_id=? AND deleted_at IS NULL AND event_type IS NULL",
                channelId, messageId, actorId);
        if (changed == 0) throw error("MESSAGE_NOT_DELETABLE", "Mesaj bulunamadı veya silme yetkiniz yok.", HttpStatus.NOT_FOUND);
        events.publishEvent(new ChannelLiveEvent(channelId));
    }

    private void requireMembershipLock(Long channelId, Long actorId) {
        users.findByIdForUpdate(actorId).filter(user -> user.isActive() && user.getDeletedAt() == null)
                .orElseThrow(() -> error("USER_NOT_ACTIVE", "Oturum geçersiz.", HttpStatus.UNAUTHORIZED));
        channels.requireMember(channelId, actorId);
        var locked = jdbc.query("SELECT user_id FROM channel_members WHERE channel_id=? AND user_id=? FOR UPDATE",
                (rs, n) -> rs.getLong(1), channelId, actorId);
        if (locked.isEmpty()) throw error("CHANNEL_MEMBERSHIP_REQUIRED", "Kanala katılmanız gerekiyor.", HttpStatus.FORBIDDEN);
    }
    private MessageView getMessage(Long channelId, Long id) {
        return jdbc.queryForObject(SELECT + " WHERE m.channel_id=? AND m.id=?", MAPPER, channelId, id);
    }
    private static String validate(String body) {
        if (body == null || body.isBlank() || body.length() > 4000) throw error("INVALID_MESSAGE", "Mesaj 1–4000 karakter olmalı.", HttpStatus.BAD_REQUEST);
        return body.trim();
    }
    private static ProjectException error(String code, String message, HttpStatus status) { return new ProjectException(code, message, status); }
}
