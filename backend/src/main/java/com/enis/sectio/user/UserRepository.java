package com.enis.sectio.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String normalizedEmail);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    boolean existsByEmail(String normalizedEmail);

    boolean existsByRoleAndDeletedAtIsNull(Role role);

    long countByRoleAndActiveTrue(Role role);

    List<User> findByActiveTrueOrderByNameAsc();

    List<User> findByDeletedAtIsNullOrderByNameAsc();

    List<User> findByDeletedAtIsNotNullOrderByNameAsc();
}
