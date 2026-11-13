package com.enis.sectio.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    List<ProjectMember> findByProjectIdOrderByUserNameAsc(Long projectId);
    boolean existsByProjectIdAndUserId(Long projectId, Long userId);
    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);
    boolean existsByProjectIdAndUserEmail(Long projectId, String email);
    boolean existsByUserId(Long userId);
    @Modifying
    @Query("delete from ProjectMember member where member.project.id = :projectId")
    void deleteByProjectId(@Param("projectId") Long projectId);
}
