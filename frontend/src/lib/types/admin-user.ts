export type AdminUserStatus = "ACTIVE" | "LOCKED" | "DISABLED";
export type StaffRole = "ADMIN" | "SUPER_ADMIN";

export interface AdminUser {
  uuid: string;
  email: string;
  phone: string | null;
  status: AdminUserStatus;
  roles: string[];
  mustChangePassword: boolean;
  lastLoginAt: string | null;
  createdAt: string;
}

export interface AdminUserCreateResult {
  user: AdminUser;
  temporaryPassword: string;
}

export interface Permission {
  code: string;
  description: string | null;
  granted: boolean;
}
