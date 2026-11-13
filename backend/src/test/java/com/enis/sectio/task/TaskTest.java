package com.enis.sectio.task;

import com.enis.sectio.project.Project;
import com.enis.sectio.user.Role;
import com.enis.sectio.user.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskTest {
    @Test
    void completionTimestampFollowsDoneStatus() {
        User user = new User("Employee", "employee@test.local", "hash", Role.EMPLOYEE);
        Project project = new Project(user, "Project", null);
        Task task = new Task(project, user, user, "Task", null, TaskPriority.MEDIUM, null);

        task.changeStatus(TaskStatus.DONE);
        assertThat(task.getCompletedAt()).isNotNull();

        task.changeStatus(TaskStatus.IN_PROGRESS);
        assertThat(task.getCompletedAt()).isNull();
    }
}
