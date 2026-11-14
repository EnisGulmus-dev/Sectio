package com.enis.sectio.dashboard;

import com.enis.sectio.task.TaskRepository;
import com.enis.sectio.task.TaskStatus;
import com.enis.sectio.user.Role;
import com.enis.sectio.user.User;
import com.enis.sectio.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final TaskRepository tasks;
    private final UserRepository users;

    public DashboardController(TaskRepository tasks, UserRepository users) {
        this.tasks = tasks;
        this.users = users;
    }

    @GetMapping("/summary")
    DashboardSummary summary(Authentication authentication) {
        User actor = users.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (actor.getRole() == Role.ADMIN) {
            return new DashboardSummary(
                    tasks.countByStatusNotIn(List.of(TaskStatus.DONE, TaskStatus.CANCELLED)),
                    tasks.countByStatus(TaskStatus.DONE),
                    tasks.countByDueAtBeforeAndStatusNotIn(Instant.now(), List.of(TaskStatus.DONE, TaskStatus.CANCELLED)));
        }
        return new DashboardSummary(
                tasks.countDistinctByAssigneesEmailAndStatusNotIn(actor.getEmail(), List.of(TaskStatus.DONE, TaskStatus.CANCELLED)),
                tasks.countDistinctByAssigneesEmailAndStatus(actor.getEmail(), TaskStatus.DONE),
                tasks.countDistinctByAssigneesEmailAndDueAtBeforeAndStatusNotIn(actor.getEmail(), Instant.now(), List.of(TaskStatus.DONE, TaskStatus.CANCELLED)));
    }

    public record DashboardSummary(long openTasks, long completedTasks, long overdueTasks) {}
}
