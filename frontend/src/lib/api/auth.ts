import { apiClient } from "@/lib/api/client";
import type { UserMe } from "@/lib/types/api";

export interface LoginPayload {
  email: string;
  password: string;
}

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
}

export const authApi = {
  login: (payload: LoginPayload) => apiClient.post<UserMe>("/auth/login", payload),
  logout: () => apiClient.post<null>("/auth/logout"),
  me: () => apiClient.get<UserMe>("/auth/me"),
  forgotPassword: (email: string) => apiClient.post<null>("/auth/forgot-password", { email }),
  resetPassword: (token: string, newPassword: string) =>
    apiClient.post<null>("/auth/reset-password", { token, newPassword }),
  changePassword: (payload: ChangePasswordPayload) =>
    apiClient.post<null>("/auth/change-password", payload),
};
