package com.enis.sectio.user.admin;

import com.enis.sectio.task.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Locale;

/** Read model: live task assignments plus preserved offboarding assignment snapshots. */
@Repository
public class UserWorkTrackingRepository {
    private static final String ENTRIES = """
            WITH entries AS (
                SELECT 'current:' || t.id AS entry_id, t.id AS task_id, p.id AS project_id,
                       t.title AS task_title, p.name AS project_name, t.status,
                       'CURRENT' AS assignment_state,
                       COALESCE(t.completed_at, t.updated_at, t.created_at) AS activity_at,
                       t.completed_at, NULL::varchar AS end_reason
                FROM task_assignees a JOIN tasks t ON t.id=a.task_id
                JOIN projects p ON p.id=t.project_id WHERE a.user_id=:userId
                UNION ALL
                SELECT 'ended:' || h.id, h.task_id, h.project_id, h.task_title, h.project_name,
                       h.status_at_end, 'ENDED', h.ended_at, NULL::timestamptz, h.reason
                FROM task_assignment_history h WHERE h.user_id=:userId
            )
            """;
    private static final String FILTER = """
            WHERE (:scope='ALL' OR assignment_state=:scope)
              AND (:status='' OR status=:status)
              AND (:query='' OR lower(task_title) LIKE :search OR lower(project_name) LIKE :search)
            """;
    private final NamedParameterJdbcTemplate jdbc;

    public UserWorkTrackingRepository(DataSource dataSource) {
        jdbc = new NamedParameterJdbcTemplate(dataSource);
    }

    public Page<UserWorkTrackingEntry> find(Long userId, UserWorkTrackingScope scope, TaskStatus status,
                                           String query, Pageable pageable) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        var parameters = new MapSqlParameterSource().addValue("userId", userId)
                .addValue("scope", scope.name()).addValue("status", status == null ? "" : status.name())
                .addValue("query", normalized).addValue("search", "%" + normalized + "%")
                .addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        Long total = jdbc.queryForObject(ENTRIES + "SELECT count(*) FROM entries " + FILTER, parameters, Long.class);
        var content = jdbc.query(ENTRIES + "SELECT * FROM entries " + FILTER
                        + " ORDER BY activity_at DESC, assignment_state, entry_id DESC LIMIT :limit OFFSET :offset",
                parameters, (row, index) -> new UserWorkTrackingEntry(row.getString("entry_id"),
                        row.getObject("task_id", Long.class), row.getObject("project_id", Long.class),
                        row.getString("task_title"), row.getString("project_name"), row.getString("status"),
                        row.getString("assignment_state"), instant(row, "activity_at"), instant(row, "completed_at"),
                        row.getString("end_reason")));
        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    private static Instant instant(ResultSet row, String column) throws SQLException {
        var value = row.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
