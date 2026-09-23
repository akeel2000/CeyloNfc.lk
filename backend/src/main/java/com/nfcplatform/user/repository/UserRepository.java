package com.nfcplatform.user.repository;

import com.nfcplatform.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    /**
     * Used to fan out admin-facing notifications (new lead, new support ticket) - deliberately
     * role-based rather than permission-based to keep the notification fan-out simple; it is
     * not a substitute for the LEAD_MANAGE/SUPPORT_MANAGE @PreAuthorize checks on the actual
     * data endpoints, which remain the real access-control boundary.
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.code IN :codes AND u.deletedAt IS NULL")
    List<User> findAllByRoleCodesIn(@Param("codes") Collection<String> codes);

    Optional<User> findByUuidAndDeletedAtIsNull(String uuid);

    boolean existsByEmailIgnoreCase(String email);

    /**
     * Fetch-joins roles and permissions in one query so callers outside an open
     * Hibernate session (e.g. JwtAuthenticationFilter, on every request) can safely
     * build a UserPrincipal without hitting LazyInitializationException.
     */
    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.roles r " +
            "LEFT JOIN FETCH r.permissions " +
            "WHERE u.uuid = :uuid AND u.deletedAt IS NULL")
    Optional<User> findByUuidWithRolesAndPermissions(@Param("uuid") String uuid);

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.roles r " +
            "LEFT JOIN FETCH r.permissions " +
            "WHERE LOWER(u.email) = LOWER(:email) AND u.deletedAt IS NULL")
    Optional<User> findByEmailWithRolesAndPermissions(@Param("email") String email);

    /** Platform staff (ADMIN/SUPER_ADMIN) only - CLIENT-role users are managed via /admin/clients. */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r " +
            "WHERE r.code IN ('ADMIN', 'SUPER_ADMIN') AND u.deletedAt IS NULL " +
            "AND (:search IS NULL OR LOWER(u.email) LIKE :search)")
    Page<User> searchAdminUsers(@Param("search") String search, Pageable pageable);

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r " +
            "WHERE u.uuid = :uuid AND r.code IN ('ADMIN', 'SUPER_ADMIN') AND u.deletedAt IS NULL")
    Optional<User> findAdminUserByUuid(@Param("uuid") String uuid);
}
