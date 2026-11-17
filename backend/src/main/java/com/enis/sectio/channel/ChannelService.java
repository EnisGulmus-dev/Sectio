package com.enis.sectio.channel;

import com.enis.sectio.user.Role;
import com.enis.sectio.user.User;
import com.enis.sectio.user.UserRepository;
import com.enis.sectio.project.ProjectException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
public class ChannelService {
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final org.springframework.context.ApplicationEventPublisher events;
    public ChannelService(JdbcTemplate jdbc, UserRepository users, org.springframework.context.ApplicationEventPublisher events) { this.jdbc = jdbc; this.users = users; this.events = events; }

    public record ChannelView(Long id, String name, String description, String visibility, Long ownerId, boolean member, Instant createdAt) {}
    public record MemberView(Long userId, String name, String jobTitle, Instant joinedAt) {}
    public record CandidateView(Long userId, String name, String jobTitle) {}

    @Transactional(readOnly = true)
    public List<CandidateView> candidates(Long channelId, Long actorId) {
        requireOwner(channelId, actorId);
        return jdbc.query("""
            SELECT u.id,u.name,u.job_title FROM users u WHERE u.active=true AND u.deleted_at IS NULL
              AND NOT EXISTS (SELECT 1 FROM channel_members m WHERE m.channel_id=? AND m.user_id=u.id)
            ORDER BY lower(u.name),u.id
            """, (rs, n) -> new CandidateView(rs.getLong("id"), rs.getString("name"), rs.getString("job_title")), channelId);
    }

    @Transactional(readOnly = true)
    public List<ChannelView> list(Long actorId) {
        actor(actorId);
        return jdbc.query("""
            SELECT c.id,c.name,c.description,c.visibility,c.owner_id,c.created_at,(m.user_id IS NOT NULL) member
            FROM channels c LEFT JOIN channel_members m ON m.channel_id=c.id AND m.user_id=?
            WHERE c.scope='CORPORATE' AND (c.visibility='PUBLIC' OR m.user_id IS NOT NULL)
            ORDER BY lower(c.name),c.id
            """, (rs, n) -> new ChannelView(rs.getLong("id"), rs.getString("name"), rs.getString("description"), rs.getString("visibility"),
                rs.getLong("owner_id"), rs.getBoolean("member"), rs.getTimestamp("created_at").toInstant()), actorId);
    }

    @Transactional
    public ChannelView create(Long actorId, String name, String description, String visibility) {
        User user = lockActor(actorId);
        if (user.getRole() != Role.ADMIN && !user.getJobTitle().canCreateProjects()) throw error("CHANNEL_CREATE_DENIED", "Kanal oluşturma yetkiniz yok.", HttpStatus.FORBIDDEN);
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty() || normalized.length() > 100) throw error("INVALID_CHANNEL_NAME", "Kanal adı gerekli ve en fazla 100 karakter olmalı.", HttpStatus.BAD_REQUEST);
        if (!"PUBLIC".equals(visibility) && !"PRIVATE".equals(visibility)) throw error("INVALID_VISIBILITY", "Geçersiz kanal türü.", HttpStatus.BAD_REQUEST);
        if (description != null && description.length() > 500) throw error("INVALID_DESCRIPTION", "Açıklama en fazla 500 karakter olmalı.", HttpStatus.BAD_REQUEST);
        Long id = jdbc.queryForObject("INSERT INTO channels(name,description,visibility,owner_id) VALUES (?,?,?,?) RETURNING id", Long.class,
                normalized, description == null || description.isBlank() ? null : description.trim(), visibility, actorId);
        jdbc.update("INSERT INTO channel_members(channel_id,user_id) VALUES (?,?)", id, actorId);
        return get(id, actorId);
    }

    @Transactional(readOnly = true)
    public ChannelView get(Long channelId, Long actorId) {
        actor(actorId);
        var found = list(actorId).stream().filter(channel -> channel.id().equals(channelId)).findFirst();
        return found.orElseThrow(() -> error("CHANNEL_NOT_FOUND", "Kanal bulunamadı.", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<MemberView> members(Long channelId, Long actorId) {
        requireMember(channelId, actorId);
        return jdbc.query("""
            SELECT u.id,u.name,u.job_title,m.joined_at FROM channel_members m JOIN users u ON u.id=m.user_id
            WHERE m.channel_id=? AND u.active=true AND u.deleted_at IS NULL ORDER BY lower(u.name),u.id
            """, (rs, n) -> new MemberView(rs.getLong("id"), rs.getString("name"), rs.getString("job_title"), rs.getTimestamp("joined_at").toInstant()), channelId);
    }

    @Transactional
    public void join(Long channelId, Long actorId) {
        actor(actorId);
        ChannelView channel = get(channelId, actorId);
        if (!"PUBLIC".equals(channel.visibility())) throw error("CHANNEL_NOT_FOUND", "Kanal bulunamadı.", HttpStatus.NOT_FOUND);
        jdbc.update("INSERT INTO channel_members(channel_id,user_id) VALUES (?,?) ON CONFLICT DO NOTHING", channelId, actorId);
    }

    @Transactional
    public void addMember(Long channelId, Long actorId, Long userId) {
        requireOwner(channelId, actorId);
        User user = users.findByIdForUpdate(userId).orElseThrow(() -> error("USER_NOT_FOUND", "Kullanıcı bulunamadı.", HttpStatus.NOT_FOUND));
        if (!user.isActive() || user.getDeletedAt() != null) throw error("USER_INACTIVE", "Yalnız aktif kullanıcılar eklenebilir.", HttpStatus.CONFLICT);
        jdbc.update("INSERT INTO channel_members(channel_id,user_id) VALUES (?,?) ON CONFLICT DO NOTHING", channelId, userId);
    }

    @Transactional
    public void removeMember(Long channelId, Long actorId, Long userId) {
        ChannelView channel = get(channelId, actorId);
        if (!channel.member()) throw error("CHANNEL_NOT_FOUND", "Kanal bulunamadı.", HttpStatus.NOT_FOUND);
        if (!channel.ownerId().equals(actorId) && !actorId.equals(userId)) throw error("CHANNEL_OWNER_REQUIRED", "Üyeleri yalnız kanal sahibi yönetebilir.", HttpStatus.FORBIDDEN);
        if (channel.ownerId().equals(userId)) throw error("CHANNEL_OWNER_REQUIRED", "Kanal sahibi kanaldan çıkarılamaz.", HttpStatus.CONFLICT);
        int removed = jdbc.update("DELETE FROM channel_members WHERE channel_id=? AND user_id=?", channelId, userId);
        if (removed > 0) {
            jdbc.update("INSERT INTO channel_messages(channel_id,author_id,body,event_type,subject_user_id) VALUES (?,?,?, ?,?)",
                    channelId, actorId, "", actorId.equals(userId) ? "MEMBER_LEFT" : "MEMBER_REMOVED", userId);
            events.publishEvent(new ChannelLiveEvent(channelId));
        }
    }

    @Transactional
    public void transferOwnership(Long channelId, Long actorId, Long nextOwnerId) {
        Long first = Math.min(actorId, nextOwnerId), second = Math.max(actorId, nextOwnerId);
        lockActor(first);
        if (!first.equals(second)) lockActor(second);
        requireOwner(channelId, actorId);
        User target = actor(nextOwnerId);
        if (!target.isActive() || target.getDeletedAt() != null) throw error("USER_INACTIVE", "Yeni sahip aktif olmalı.", HttpStatus.CONFLICT);
        Integer count = jdbc.queryForObject("SELECT count(*) FROM channel_members WHERE channel_id=? AND user_id=?", Integer.class, channelId, nextOwnerId);
        if (count == null || count == 0) throw error("CHANNEL_MEMBERSHIP_REQUIRED", "Yeni sahip kanal üyesi olmalı.", HttpStatus.CONFLICT);
        jdbc.update("UPDATE channels SET owner_id=? WHERE id=? AND owner_id=?", nextOwnerId, channelId, actorId);
    }

    @Transactional
    public void delete(Long channelId, Long actorId) {
        requireOwner(channelId, actorId);
        jdbc.update("DELETE FROM channels WHERE id=? AND owner_id=?", channelId, actorId);
        events.publishEvent(new ChannelLiveEvent(channelId));
    }

    @Transactional(readOnly = true)
    public void requireMember(Long channelId, Long actorId) {
        ChannelView channel = get(channelId, actorId);
        if (!channel.member()) throw error("CHANNEL_MEMBERSHIP_REQUIRED", "Kanala katılmanız gerekiyor.", HttpStatus.FORBIDDEN);
    }
    private void requireOwner(Long channelId, Long actorId) {
        ChannelView channel = get(channelId, actorId);
        if (!channel.member() || !channel.ownerId().equals(actorId)) throw error("CHANNEL_OWNER_REQUIRED", "Üyeleri yalnız kanal sahibi yönetebilir.", HttpStatus.FORBIDDEN);
    }
    private User actor(Long id) {
        return users.findById(id).filter(user -> user.isActive() && user.getDeletedAt() == null)
                .orElseThrow(() -> error("USER_NOT_ACTIVE", "Oturum geçersiz.", HttpStatus.UNAUTHORIZED));
    }
    private User lockActor(Long id) {
        return users.findByIdForUpdate(id).filter(user -> user.isActive() && user.getDeletedAt() == null)
                .orElseThrow(() -> error("USER_NOT_ACTIVE", "Hesap aktif değil.", HttpStatus.CONFLICT));
    }
    private static ProjectException error(String code, String message, HttpStatus status) { return new ProjectException(code, message, status); }
}
