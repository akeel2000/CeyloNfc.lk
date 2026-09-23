package com.nfcplatform.permission;

/**
 * Central registry of permission codes (mirrors docs/ROLE_PERMISSION_MATRIX.md and the
 * V3 seed migration) so @PreAuthorize expressions reference a constant instead of a
 * hand-typed string literal that could drift from the seeded catalog.
 */
public final class PermissionCodes {

    private PermissionCodes() {
    }

    public static final String CLIENT_VIEW = "CLIENT_VIEW";
    public static final String CLIENT_CREATE = "CLIENT_CREATE";
    public static final String CLIENT_UPDATE = "CLIENT_UPDATE";
    public static final String CLIENT_DELETE = "CLIENT_DELETE";
    public static final String CLIENT_SUSPEND = "CLIENT_SUSPEND";

    public static final String NFC_VIEW = "NFC_VIEW";
    public static final String NFC_CREATE = "NFC_CREATE";
    public static final String NFC_UPDATE = "NFC_UPDATE";
    public static final String NFC_ASSIGN = "NFC_ASSIGN";
    public static final String NFC_ACTIVATE = "NFC_ACTIVATE";
    public static final String NFC_SUSPEND = "NFC_SUSPEND";

    public static final String DESTINATION_MANAGE = "DESTINATION_MANAGE";
    public static final String PROFILE_MANAGE = "PROFILE_MANAGE";
    public static final String QR_MANAGE = "QR_MANAGE";
    public static final String GOOGLE_REVIEW_MANAGE = "GOOGLE_REVIEW_MANAGE";
    public static final String MENU_MANAGE = "MENU_MANAGE";
    public static final String TEMPLATE_MANAGE = "TEMPLATE_MANAGE";
    public static final String PACKAGE_MANAGE = "PACKAGE_MANAGE";
    public static final String SUBSCRIPTION_MANAGE = "SUBSCRIPTION_MANAGE";
    public static final String ORDER_MANAGE = "ORDER_MANAGE";
    public static final String ANALYTICS_VIEW = "ANALYTICS_VIEW";
    public static final String SUPPORT_MANAGE = "SUPPORT_MANAGE";
    public static final String LEAD_MANAGE = "LEAD_MANAGE";
    public static final String AUDIT_VIEW = "AUDIT_VIEW";
    public static final String SETTINGS_MANAGE = "SETTINGS_MANAGE";
}
