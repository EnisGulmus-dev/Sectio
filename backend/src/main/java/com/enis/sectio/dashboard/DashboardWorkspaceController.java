package com.enis.sectio.dashboard;

import com.enis.sectio.user.*;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
public class DashboardWorkspaceController {
    private final EntityManager em;
    private final UserRepository users;
    public DashboardWorkspaceController(EntityManager em, UserRepository users) { this.em = em; this.users = users; }

    @GetMapping("/api/v1/dashboard/workspace")
    @Transactional(readOnly = true)
    public Workspace workspace(Authentication authentication) {
        var actor = users.findByEmail(authentication.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        boolean admin = actor.getRole() == Role.ADMIN;
        boolean leader = actor.getJobTitle() == JobTitle.TEAM_LEADER;
        boolean manager = actor.getJobTitle().canCreateProjects();
        String scope = admin ? "true" : "(p.owner_id = :uid or exists (select 1 from project_members pm where pm.project_id = p.id and pm.user_id = :uid))";
        String taskScope = manager || admin ? scope : "exists (select 1 from task_assignees a where a.task_id = t.id and a.user_id = :uid)";
        String fromTasks = " from tasks t join projects p on p.id = t.project_id where p.archived_at is null and " + taskScope;
        var counts = rows("select count(*) filter (where t.status not in ('DONE','CANCELLED')), count(*) filter (where t.status='DONE'), count(*) filter (where t.status not in ('DONE','CANCELLED') and t.due_at < current_timestamp)" + fromTasks, actor.getId()).getFirst();
        var deadlines = rows("select t.id,t.title,p.id,p.name,cast(t.due_at as text),t.status" + fromTasks + " and t.status not in ('DONE','CANCELLED') order by t.due_at asc nulls last,t.id desc limit 8", actor.getId()).stream().map(r -> new Deadline(n(r[0]), (String)r[1], n(r[2]), (String)r[3], (String)r[4], (String)r[5])).toList();
        List<Progress> progress = manager || admin ? rows("select p.id,p.name,count(t.id),count(t.id) filter (where t.status='DONE'),count(t.id) filter (where t.status='CANCELLED') from projects p left join tasks t on t.project_id=p.id where p.archived_at is null and " + scope + " group by p.id,p.name,p.updated_at order by p.updated_at desc limit 8", actor.getId()).stream().map(r -> new Progress(n(r[0]), (String)r[1], n(r[2]), n(r[3]), n(r[4]))).toList() : List.of();
        List<Workload> teams = leader ? rows("select team.id,team.name,count(distinct tm.user_id),count(distinct t.id) filter (where t.status not in ('DONE','CANCELLED')),count(distinct t.id) filter (where t.status not in ('DONE','CANCELLED') and t.due_at < current_timestamp) from project_teams team join projects p on p.id=team.project_id left join project_team_members tm on tm.team_id=team.id left join task_assignees a on a.user_id=tm.user_id left join tasks t on t.id=a.task_id and t.project_id=p.id where p.archived_at is null and " + scope + " and (p.owner_id=:uid or exists (select 1 from project_team_members mine where mine.team_id=team.id and mine.user_id=:uid)) group by team.id,team.name order by count(distinct t.id) filter (where t.status not in ('DONE','CANCELLED')) desc,team.id limit 8", actor.getId()).stream().map(r -> new Workload(n(r[0]), (String)r[1], n(r[2]), n(r[3]), n(r[4]))).toList() : List.of();
        SystemSummary system = null;
        if (admin) {
            var r = rows("select (select count(*) from users where deleted_at is null),(select count(*) from users where active=true and deleted_at is null),(select count(*) from projects where archived_at is null),(select count(*) from projects where archived_at is not null)", actor.getId()).getFirst();
            system = new SystemSummary(n(r[0]),n(r[1]),n(r[2]),n(r[3]));
        }
        return new Workspace(n(counts[0]),n(counts[1]),n(counts[2]),deadlines,progress,teams,system);
    }
    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String sql, Long uid) {
        var query = em.createNativeQuery(sql);
        if (sql.contains(":uid")) query.setParameter("uid",uid);
        return query.getResultList();
    }
    private static long n(Object v) { return ((Number)v).longValue(); }
    public record Deadline(long id,String title,long projectId,String projectName,String dueAt,String status) {}
    public record Progress(long id,String name,long total,long completed,long cancelled) {}
    public record Workload(long id,String name,long members,long open,long overdue) {}
    public record SystemSummary(long users,long activeUsers,long activeProjects,long archivedProjects) {}
    public record Workspace(long openTasks,long completedTasks,long overdueTasks,List<Deadline> deadlines,List<Progress> projects,List<Workload> teams,SystemSummary system) {}
}
