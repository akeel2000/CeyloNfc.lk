package com.nfcplatform.security;

import com.nfcplatform.permission.repository.AdminPermissionOverrideRepository;
import com.nfcplatform.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Builds a UserPrincipal with role-derived permissions merged with any per-admin
 * overrides (admin_permission_overrides) - the single place this merge happens so
 * every authentication path (login, refresh, per-request JWT filter) is consistent.
 */
@Component
@RequiredArgsConstructor
public class PrincipalFactory {

    private final AdminPermissionOverrideRepository overrideRepository;

    public UserPrincipal build(User user) {
        var overrides = overrideRepository.findGrantedPermissionCodesByUserId(user.getId());
        return new UserPrincipal(user, overrides);
    }
}
