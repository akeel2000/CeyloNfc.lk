-- Static reference data: roles and the permission catalog defined in
-- docs/ROLE_PERMISSION_MATRIX.md. User accounts (Super Admin, etc.) are NOT
-- seeded here — they are created at application startup from environment
-- variables (see DataSeedRunner), so no fixed credential ever ships in a migration.

INSERT INTO roles (uuid, code, name, description) VALUES
    (UUID(), 'SUPER_ADMIN', 'Super Admin', 'Full, unrestricted platform access'),
    (UUID(), 'ADMIN', 'Admin', 'Operational access, permissions granted individually'),
    (UUID(), 'CLIENT', 'Client', 'Customer who owns NFC cards and a digital profile');

INSERT INTO permissions (code, description) VALUES
    ('CLIENT_VIEW', 'View client accounts'),
    ('CLIENT_CREATE', 'Create client accounts'),
    ('CLIENT_UPDATE', 'Edit client accounts'),
    ('CLIENT_DELETE', 'Delete client accounts'),
    ('CLIENT_SUSPEND', 'Suspend/reinstate client accounts'),
    ('NFC_VIEW', 'View NFC cards'),
    ('NFC_CREATE', 'Register NFC cards'),
    ('NFC_UPDATE', 'Edit NFC cards'),
    ('NFC_ASSIGN', 'Assign NFC cards to clients'),
    ('NFC_ACTIVATE', 'Activate NFC cards'),
    ('NFC_SUSPEND', 'Suspend NFC cards'),
    ('DESTINATION_MANAGE', 'Manage destinations'),
    ('PROFILE_MANAGE', 'Manage client profiles'),
    ('QR_MANAGE', 'Manage QR codes'),
    ('GOOGLE_REVIEW_MANAGE', 'Manage Google Review locations'),
    ('MENU_MANAGE', 'Manage menus'),
    ('TEMPLATE_MANAGE', 'Manage templates'),
    ('PACKAGE_MANAGE', 'Manage package plans'),
    ('SUBSCRIPTION_MANAGE', 'Manage subscriptions'),
    ('ORDER_MANAGE', 'Manage orders'),
    ('ANALYTICS_VIEW', 'View analytics'),
    ('SUPPORT_MANAGE', 'Manage support tickets'),
    ('LEAD_MANAGE', 'Manage sales leads'),
    ('AUDIT_VIEW', 'View audit logs'),
    ('SETTINGS_MANAGE', 'Manage platform settings');
