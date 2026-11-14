package com.enis.sectio.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectTeamRepository extends JpaRepository<ProjectTeam, Long> {
    List<ProjectTeam> findByProjectIdOrderByNameAsc(Long projectId);
    Optional<ProjectTeam> findByIdAndProjectId(Long id, Long projectId);
    boolean existsByProjectIdAndNameIgnoreCase(Long projectId, String name);

    @Modifying
    @Query(value = "DELETE FROM project_team_members WHERE user_id = :userId", nativeQuery = true)
    void deleteMembershipsByUserId(@Param("userId") Long userId);

    @Modifying
    @Query(value = "DELETE FROM project_team_members WHERE user_id = :userId AND team_id IN (SELECT id FROM project_teams WHERE project_id = :projectId)", nativeQuery = true)
    void deleteMembershipsByProjectAndUser(@Param("projectId") Long projectId, @Param("userId") Long userId);
}
