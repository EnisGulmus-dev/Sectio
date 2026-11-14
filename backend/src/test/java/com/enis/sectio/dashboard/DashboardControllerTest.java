package com.enis.sectio.dashboard;

import com.enis.sectio.task.TaskRepository;
import com.enis.sectio.task.TaskStatus;
import com.enis.sectio.user.Role;
import com.enis.sectio.user.User;
import com.enis.sectio.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DashboardControllerTest {
    @Test
    void employeeOnlySeesOwnTaskCounters() {
        TaskRepository tasks = mock(TaskRepository.class);
        UserRepository users = mock(UserRepository.class);
        User employee = new User("Employee", "employee@test.local", "hash", Role.EMPLOYEE);
        when(users.findByEmail(employee.getEmail())).thenReturn(Optional.of(employee));
        when(tasks.countDistinctByAssigneesEmailAndStatusNotIn(eq(employee.getEmail()), anyCollection())).thenReturn(3L);
        when(tasks.countDistinctByAssigneesEmailAndStatus(employee.getEmail(), TaskStatus.DONE)).thenReturn(2L);
        when(tasks.countDistinctByAssigneesEmailAndDueAtBeforeAndStatusNotIn(eq(employee.getEmail()), any(Instant.class), anyCollection())).thenReturn(1L);

        var summary = new DashboardController(tasks, users).summary(
                new UsernamePasswordAuthenticationToken(employee.getEmail(), "ignored"));

        assertThat(summary.openTasks()).isEqualTo(3);
        assertThat(summary.completedTasks()).isEqualTo(2);
        assertThat(summary.overdueTasks()).isEqualTo(1);
        verify(tasks, never()).countByStatus(any());
    }
}
