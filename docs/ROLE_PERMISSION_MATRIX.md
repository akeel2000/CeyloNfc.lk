# Role / Permission Matrix

`SUPER_ADMIN` implicitly holds every permission below (enforced in code, not by seeding every
row). `ADMIN` permissions are explicit grants in `admin_permission_overrides`, configurable per
admin by a Super Admin. `CLIENT` never holds platform permissions — client access is instead
scoped by ownership (`client_id`) checks in the service layer, not the permission table.

| Permission              | Super Admin | Admin (configurable) | Client (ownership-scoped) |
|--------------------------|:-----------:|:---------------------:|:--------------------------:|
| CLIENT_VIEW              | YES | permission | own only |
| CLIENT_CREATE            | YES | permission | no |
| CLIENT_UPDATE            | YES | permission | own only (profile fields) |
| CLIENT_DELETE            | YES | permission | no |
| CLIENT_SUSPEND           | YES | permission | no |
| NFC_VIEW                 | YES | permission | own cards only |
| NFC_CREATE                | YES | permission | no |
| NFC_UPDATE                | YES | permission | no |
| NFC_ASSIGN                | YES | permission | no |
| NFC_ACTIVATE               | YES | permission | no |
| NFC_SUSPEND                | YES | permission | no |
| DESTINATION_MANAGE          | YES | permission | own destinations only |
| PROFILE_MANAGE               | YES | permission | own profile only |
| QR_MANAGE                     | YES | permission | own QR codes only |
| GOOGLE_REVIEW_MANAGE            | YES | permission | own locations only |
| MENU_MANAGE                      | YES | permission | own menus only |
| TEMPLATE_MANAGE                    | YES | permission | select/apply only |
| PACKAGE_MANAGE                      | YES | permission | no |
| SUBSCRIPTION_MANAGE                   | YES | permission | view own only |
| ORDER_MANAGE                            | YES | permission | view own only |
| ANALYTICS_VIEW                            | YES | permission | own analytics only |
| SUPPORT_MANAGE                              | YES | permission | own tickets only |
| LEAD_MANAGE                                   | YES | permission | no |
| AUDIT_VIEW                                      | YES | permission | no |
| SETTINGS_MANAGE                                   | YES | permission | no |

"permission" = grantable per-admin via `admin_permission_overrides`; absent by default until
a Super Admin grants it. "own only" = enforced server-side via `client_id` matching the
authenticated principal's client, never via a frontend-supplied parameter.
