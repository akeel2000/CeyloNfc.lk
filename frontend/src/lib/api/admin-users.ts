import { apiClient } from "@/lib/api/client";
import type { AdminUser, AdminUserCreateResult, Permission } from "@/lib/types/admin-user";
import type { PageResponse } from "@/lib/types/client";
import type { AdminUserCreateFormValues } from "@/lib/schemas/admin-user";

export interface AdminUserListParams {
  page?: number;
  size?: number;
  search?: string;
}

function buildQuery(params: AdminUserListParams): string {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set("page", String(params.page));
  if (params.size !== undefined) query.set("size", String(params.size));
  if (params.search) query.set("search", params.search);
  const qs = query.toString();
  return qs ? `?${qs}` : "";
}

export const adminUsersApi = {
  list: (params: AdminUserListParams = {}) =>
    apiClient.get<PageResponse<AdminUser>>(`/admin/users${buildQuery(params)}`),
  get: (uuid: string) => apiClient.get<AdminUser>(`/admin/users/${uuid}`),
  create: (payload: AdminUserCreateFormValues) =>
    apiClient.post<AdminUserCreateResult>("/admin/users", payload),
  updateStatus: (uuid: string, status: string) =>
    apiClient.patch<AdminUser>(`/admin/users/${uuid}/status`, { status }),
  getPermissions: (uuid: string) => apiClient.get<Permission[]>(`/admin/users/${uuid}/permissions`),
  updatePermissions: (uuid: string, grantedPermissionCodes: string[]) =>
    apiClient.put<Permission[]>(`/admin/users/${uuid}/permissions`, { grantedPermissionCodes }),
};
