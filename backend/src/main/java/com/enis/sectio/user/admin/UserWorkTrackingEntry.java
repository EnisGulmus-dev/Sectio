package com.enis.sectio.user.admin;

import java.time.Instant;

public record UserWorkTrackingEntry(String id, Long taskId, Long projectId, String taskTitle,
                                    String projectName, String status, String assignmentState,
                                    Instant activityAt, Instant completedAt, String endReason) {}
