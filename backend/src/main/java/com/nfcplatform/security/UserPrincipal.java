package com.nfcplatform.security;

import com.nfcplatform.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String uuid;
    private final String email;
    private final String passwordHash;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final Set<String> roleCodes;
    private final Set<String> permissionCodes;

    /** Role-derived permissions only. Prefer {@link #UserPrincipal(User, Set)} via PrincipalFactory
     *  so per-admin overrides (admin_permission_overrides) are included. */
    public UserPrincipal(User user) {
        this(user, Set.of());
    }

    public UserPrincipal(User user, Set<String> overridePermissionCodes) {
        this.id = user.getId();
        this.uuid = user.getUuid();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.enabled = user.getStatus() == com.nfcplatform.user.entity.UserStatus.ACTIVE;
        this.accountNonLocked = !user.isAccountLocked();
        this.roleCodes = user.getRoles().stream()
                .map(role -> role.getCode().name())
                .collect(Collectors.toUnmodifiableSet());

        Set<String> merged = new HashSet<>(overridePermissionCodes);
        user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(com.nfcplatform.permission.entity.Permission::getCode)
                .forEach(merged::add);
        this.permissionCodes = Set.copyOf(merged);
    }

    public boolean isSuperAdmin() {
        return roleCodes.contains("SUPER_ADMIN");
    }

    public boolean hasPermission(String permissionCode) {
        return isSuperAdmin() || permissionCodes.contains(permissionCode);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = roleCodes.stream()
                .map(code -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + code))
                .collect(Collectors.toCollection(HashSet::new));
        permissionCodes.forEach(code -> authorities.add(new SimpleGrantedAuthority(code)));
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
