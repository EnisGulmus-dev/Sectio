package com.enis.sectio.project;

import java.time.Instant;
import java.util.List;

public record ProjectTeamResponse(Long id, String name, List<ProjectTeamMemberResponse> members, Instant createdAt) {
    public static ProjectTeamResponse from(ProjectTeam team) {
        return new ProjectTeamResponse(
                team.getId(),
                team.getName(),
                team.getMembers().stream().map(ProjectTeamMemberResponse::from).toList(),
                team.getCreatedAt()
        );
    }

    public record ProjectTeamMemberResponse(Long userId, String name) {
        static ProjectTeamMemberResponse from(com.enis.sectio.user.User user) {
            return new ProjectTeamMemberResponse(user.getId(), user.getName());
        }
    }
}
