"use client";

import { createContext, useContext, type ReactNode } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";

import { authApi } from "@/lib/api/auth";
import { ApiClientError } from "@/lib/api/client";
import type { UserMe } from "@/lib/types/api";

interface AuthContextValue {
  user: UserMe | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  refetch: () => Promise<unknown>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/**
 * Frontend session state is a convenience for UI/routing only. The backend
 * independently re-validates identity, role and tenant ownership on every
 * protected request - this provider never substitutes for that.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const { data, isLoading, refetch } = useQuery({
    queryKey: ["auth", "me"],
    queryFn: async () => {
      try {
        return await authApi.me();
      } catch (error) {
        if (error instanceof ApiClientError && error.status === 401) {
          return null;
        }
        throw error;
      }
    },
    staleTime: 60_000,
    retry: false,
  });

  const value: AuthContextValue = {
    user: data ?? null,
    isLoading,
    isAuthenticated: Boolean(data),
    refetch,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return ctx;
}

export function useLogout() {
  const queryClient = useQueryClient();
  return async () => {
    await authApi.logout();
    queryClient.setQueryData(["auth", "me"], null);
  };
}
