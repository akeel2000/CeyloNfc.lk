package com.nfcplatform.permission.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "admin_permission_overrides")
public class AdminPermissionOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    @Column(nullable = false)
    private boolean granted = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
