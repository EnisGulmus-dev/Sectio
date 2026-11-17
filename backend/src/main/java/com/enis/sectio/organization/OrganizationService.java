package com.enis.sectio.organization;

import com.enis.sectio.project.ProjectException;
import com.enis.sectio.user.Role;
import com.enis.sectio.user.User;
import com.enis.sectio.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class OrganizationService {
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    public OrganizationService(JdbcTemplate jdbc, UserRepository users) { this.jdbc = jdbc; this.users = users; }
    public record Team(Long id, String name) {}
    public record Relationship(Long userId, Long managerId, String managerName, List<Team> teams) {}
    public record Person(Long id, String name, String jobTitle) {}

    @Transactional(readOnly = true)
    public List<Person> managers(Long actorId) {
        activeUser(actorId);
        return jdbc.query("SELECT id,name,job_title FROM users WHERE active=true AND deleted_at IS NULL AND (role='ADMIN' OR job_title IN ('TEAM_LEADER','PROJECT_MANAGER','PROJECT_OWNER')) ORDER BY lower(name),id", (r,n) -> new Person(r.getLong(1),r.getString(2),r.getString(3)));
    }
    @Transactional(readOnly = true)
    public List<Person> reports(Long actorId) {
        activeUser(actorId);
        return jdbc.query("SELECT u.id,u.name,u.job_title FROM users u JOIN company_reporting_lines r ON r.user_id=u.id WHERE r.manager_id=? AND u.active=true AND u.deleted_at IS NULL ORDER BY lower(u.name),u.id", (r,n) -> new Person(r.getLong(1),r.getString(2),r.getString(3)), actorId);
    }

    @Transactional(readOnly = true)
    public List<Team> teams(Long actorId) {
        activeUser(actorId);
        return jdbc.query("SELECT id,name FROM company_teams ORDER BY lower(name),id", (r,n) -> new Team(r.getLong(1),r.getString(2)));
    }
    @Transactional
    public Team createTeam(Long actorId, String name) {
        return createTeam(actorId, name, List.of());
    }
    @Transactional
    public Team createTeam(Long actorId, String name, List<Long> userIds) {
        lockOrganization();
        requireAdmin(actorId);
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty() || clean.length() > 100) throw error("INVALID_TEAM_NAME", "Takım adı 1–100 karakter olmalı.", HttpStatus.BAD_REQUEST);
        var selected = userIds == null ? List.<Long>of() : List.copyOf(new LinkedHashSet<>(userIds));
        for (Long userId : selected) activeUser(userId);
        Long id = jdbc.queryForObject("INSERT INTO company_teams(name) VALUES (?) RETURNING id", Long.class, clean);
        for (Long userId : selected) jdbc.update("INSERT INTO company_team_members(team_id,user_id) VALUES (?,?)", id, userId);
        return new Team(id, clean);
    }
    @Transactional
    public Team renameTeam(Long actorId, Long teamId, String name) {
        requireAdmin(actorId);
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty() || clean.length() > 100) throw error("INVALID_TEAM_NAME", "Takım adı 1–100 karakter olmalı.", HttpStatus.BAD_REQUEST);
        if (jdbc.update("UPDATE company_teams SET name=? WHERE id=?", clean, teamId) == 0) throw error("COMPANY_TEAM_NOT_FOUND", "Şirket takımı bulunamadı.", HttpStatus.NOT_FOUND);
        return new Team(teamId, clean);
    }
    @Transactional
    public void deleteTeam(Long actorId, Long teamId) {
        requireAdmin(actorId);
        if (jdbc.update("DELETE FROM company_teams WHERE id=?", teamId) == 0) throw error("COMPANY_TEAM_NOT_FOUND", "Şirket takımı bulunamadı.", HttpStatus.NOT_FOUND);
    }
    @Transactional(readOnly = true)
    public Relationship relationship(Long actorId, Long userId) {
        User actor = activeUser(actorId);
        if (actor.getRole() != Role.ADMIN && !actorId.equals(userId) && !isDirectManager(actorId, userId)) throw error("ORGANIZATION_ACCESS_DENIED", "Bu çalışanın organizasyon bilgilerini görüntüleme yetkiniz yok.", HttpStatus.FORBIDDEN);
        return read(userId);
    }
    @Transactional
    public Relationship assign(Long actorId, Long userId, Long managerId, List<Long> teamIds) {
        lockOrganization();
        User actor = activeUser(actorId);
        User target = activeUser(userId);
        if (actor.getRole() != Role.ADMIN) {
            if (!isDirectManager(actorId, userId)) throw error("ORGANIZATION_ACCESS_DENIED", "Yalnız doğrudan bağlı çalışanları yönetebilirsiniz.", HttpStatus.FORBIDDEN);
            if (!actorId.equals(managerId)) throw error("MANAGER_CHANGE_DENIED", "Yönetici değişikliğini yalnız ADMIN yapabilir.", HttpStatus.FORBIDDEN);
        }
        if (managerId != null) {
            User manager = activeUser(managerId);
            if (manager.getRole() != Role.ADMIN && !manager.getJobTitle().canCreateProjects()) throw error("INVALID_MANAGER", "Yönetici olarak yalnız yetkili yönetici veya lider seçilebilir.", HttpStatus.CONFLICT);
            if (managerId.equals(target.getId())) throw error("MANAGER_CYCLE", "Kişi kendi yöneticisi olamaz.", HttpStatus.CONFLICT);
            Boolean cycle = jdbc.queryForObject("""
                WITH RECURSIVE superiors(id) AS (
                    SELECT CAST(? AS BIGINT) UNION SELECT r.manager_id FROM company_reporting_lines r JOIN superiors s ON r.user_id=s.id
                ) SELECT EXISTS(SELECT 1 FROM superiors WHERE id=?)
                """, Boolean.class, managerId, userId);
            if (Boolean.TRUE.equals(cycle)) throw error("MANAGER_CYCLE", "Yöneticilik ilişkisi döngü oluşturamaz.", HttpStatus.CONFLICT);
        }
        var ids = teamIds == null ? List.<Long>of() : List.copyOf(new LinkedHashSet<>(teamIds));
        for (Long id : ids) if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM company_teams WHERE id=?)", Boolean.class, id))) throw error("COMPANY_TEAM_NOT_FOUND", "Şirket takımı bulunamadı.", HttpStatus.NOT_FOUND);
        jdbc.update("DELETE FROM company_reporting_lines WHERE user_id=?", userId);
        if (managerId != null) jdbc.update("INSERT INTO company_reporting_lines(user_id,manager_id) VALUES (?,?)", userId, managerId);
        jdbc.update("DELETE FROM company_team_members WHERE user_id=?", userId);
        for (Long teamId : ids) jdbc.update("INSERT INTO company_team_members(team_id,user_id) VALUES (?,?)", teamId, userId);
        return read(userId);
    }
    public void lockOrganization() { jdbc.execute("SELECT pg_advisory_xact_lock(739142019)"); }
    @Transactional
    public void removeDeletedUser(Long userId) {
        lockOrganization();
        jdbc.update("DELETE FROM company_team_members WHERE user_id=?", userId);
        jdbc.update("DELETE FROM company_reporting_lines WHERE user_id=?", userId);
    }
    @Transactional(readOnly = true)
    public boolean hasDirectReports(Long managerId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM company_reporting_lines WHERE manager_id=?)", Boolean.class, managerId));
    }
    private Relationship read(Long userId) {
        User user = users.findById(userId).orElseThrow(() -> error("USER_NOT_FOUND", "Kullanıcı bulunamadı.", HttpStatus.NOT_FOUND));
        var managers = jdbc.query("SELECT u.id,u.name FROM company_reporting_lines r JOIN users u ON u.id=r.manager_id WHERE r.user_id=?",
                (r,n) -> new Object[]{r.getLong(1),r.getString(2)}, userId);
        var teams = jdbc.query("SELECT t.id,t.name FROM company_teams t JOIN company_team_members m ON m.team_id=t.id WHERE m.user_id=? ORDER BY lower(t.name),t.id", (r,n) -> new Team(r.getLong(1),r.getString(2)), userId);
        return new Relationship(user.getId(), managers.isEmpty() ? null : (Long) managers.getFirst()[0], managers.isEmpty() ? null : (String) managers.getFirst()[1], teams);
    }
    private boolean isDirectManager(Long managerId, Long userId) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM company_reporting_lines WHERE manager_id=? AND user_id=?)", Boolean.class, managerId, userId)); }
    private void requireAdmin(Long id) { if (activeUser(id).getRole() != Role.ADMIN) throw error("ADMIN_REQUIRED", "Yalnız yöneticiler şirket takımı yönetebilir.", HttpStatus.FORBIDDEN); }
    private User activeUser(Long id) { return users.findById(id).filter(u -> u.isActive() && u.getDeletedAt() == null).orElseThrow(() -> error("USER_NOT_ACTIVE", "Aktif kullanıcı bulunamadı.", HttpStatus.CONFLICT)); }
    private static ProjectException error(String code, String message, HttpStatus status) { return new ProjectException(code, message, status); }
}
