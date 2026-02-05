package com.flowforge.repository;

import com.flowforge.domain.entity.User;
import com.flowforge.domain.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Page<User> findByIsActiveTrue(Pageable pageable);

    Page<User> findByRole(UserRole role, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.isActive = true AND " +
           "(LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.lockedUntil IS NOT NULL AND u.lockedUntil > :now")
    List<User> findLockedUsers(@Param("now") Instant now);

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime, u.failedLoginAttempts = 0, u.lockedUntil = null WHERE u.id = :userId")
    void updateLoginSuccess(@Param("userId") UUID userId, @Param("loginTime") Instant loginTime);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.id = :userId")
    void incrementFailedLoginAttempts(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE User u SET u.lockedUntil = :lockedUntil WHERE u.id = :userId")
    void lockUser(@Param("userId") UUID userId, @Param("lockedUntil") Instant lockedUntil);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.isActive = true")
    long countByRoleAndActive(@Param("role") UserRole role);

    long countByIsActiveTrue();
}
