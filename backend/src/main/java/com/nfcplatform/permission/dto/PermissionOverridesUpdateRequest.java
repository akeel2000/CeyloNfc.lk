package com.nfcplatform.permission.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

/** The complete desired set of granted permission codes for this admin - diffed server-side
 *  against the current admin_permission_overrides rows rather than applied as incremental
 *  add/remove calls, so the UI can just submit "here's the full checked list" each time. */
public record PermissionOverridesUpdateRequest(
        @NotNull Set<String> grantedPermissionCodes
) {
}
