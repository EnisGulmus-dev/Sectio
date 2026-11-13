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
                    tasks.countByStatusNot(TaskStatus.DONE),
                    tasks.countByStatus(TaskStatus.DONE),
                    tasks.countByDueAtBeforeAndStatusNot(Instant.now(), TaskStatus.DONE));
        }
        return new DashboardSummary(
                tasks.countByAssigneeEmailAndStatusNot(actor.getEmail(), TaskStatus.DONE),
                tasks.countByAssigneeEmailAndStatus(actor.getEmail(), TaskStatus.DONE),
                tasks.countByAssigneeEmailAndDueAtBeforeAndStatusNot(actor.getEmail(), Instant.now(), TaskStatus.DONE));
    }

    public record DashboardSummary(long openTasks, long completedTasks, long overdueTasks) {}
}
