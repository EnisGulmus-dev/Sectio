package com.enis.sectio.project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    boolean existsByOwnerId(Long ownerId);

    @Query("""
        select distinct p from Project p left join ProjectMember pm on pm.project = p
        where (:admin = true or p.owner.email = :email or pm.user.email = :email)
          and (:archived is null or (:archived = true and p.archivedAt is not null) or (:archived = false and p.archivedAt is null))
          and (:query = '' or lower(p.name) like lower(concat('%', :query, '%')))
        """)
    Page<Project> findAccessible(@Param("email") String email, @Param("admin") boolean admin,
                                 @Param("archived") Boolean archived, @Param("query") String query, Pageable pageable);
}
