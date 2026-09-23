package com.nfcplatform.permission.repository;

import com.nfcplatform.permission.entity.AdminPermissionOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface AdminPermissionOverrideRepository extends JpaRepository<AdminPermissionOverride, Long> {

    @Query("SELECT p.code FROM AdminPermissionOverride o JOIN Permission p ON p.id = o.permissionId " +
            "WHERE o.userId = :userId AND o.granted = true")
    Set<String> findGrantedPermissionCodesByUserId(@Param("userId") Long userId);

    List<AdminPermissionOverride> findAllByUserId(Long userId);

    void deleteAllByUserId(Long userId);
}
