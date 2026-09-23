import { env } from "@/lib/config/env";
import type { ApiResponse } from "@/lib/types/api";

export class ApiClientError extends Error {
  code: string;
  status: number;

  constructor(code: string, message: string, status: number) {
    super(message);
    this.code = code;
    this.status = status;
  }
}

function readCookie(name: string): string | null {
  if (typeof document === "undefined") return null;
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : null;
}

type RequestOptions = Omit<RequestInit, "body"> & { body?: unknown };

/**
 * Shared across every caller that hits a 401 at once (e.g. several widgets' queries
 * expiring together). The refresh token is single-use and rotates server-side, which treats
 * a second concurrent use of the same (now-already-rotated) token as reuse/compromise and
 * revokes the whole session - so every 401 firing its own /auth/refresh would intermittently
 * force a real logout under ordinary concurrent traffic, not just an actual attack. Routing
 * all concurrent refreshes through one in-flight request avoids that race entirely.
 */
let refreshPromise: Promise<boolean> | null = null;

function refreshSession(): Promise<boolean> {
  if (!refreshPromise) {
    refreshPromise = fetch(`${env.apiUrl}/auth/refresh`, { method: "POST", credentials: "include" })
      .then((res) => res.ok)
      .catch(() => false)
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

/**
 * Centralized API client. Always sends credentials (HttpOnly auth cookies),
 * attaches the CSRF header for state-changing requests, and transparently
 * retries once via /auth/refresh on a 401 before giving up.
 */
async function request<T>(path: string, options: RequestOptions = {}, isRetry = false): Promise<T> {
  const method = (options.method ?? "GET").toUpperCase();
  const isMutation = method !== "GET" && method !== "HEAD";

  const isFormData = options.body instanceof FormData;

  const headers = new Headers(options.headers);
  headers.set("Accept", "application/json");
  if (options.body !== undefined && !isFormData) {
    // FormData bodies are left for the browser to set Content-Type on (it must include the
    // multipart boundary, which we can't know here) - only JSON bodies get this header.
    headers.set("Content-Type", "application/json");
  }
  if (isMutation) {
    const csrfToken = readCookie("XSRF-TOKEN");
    if (csrfToken) {
      headers.set("X-XSRF-TOKEN", csrfToken);
    }
  }

  const response = await fetch(`${env.apiUrl}${path}`, {
    ...options,
    method,
    headers,
    credentials: "include",
    body: isFormData ? (options.body as FormData) : options.body !== undefined ? JSON.stringify(options.body) : undefined,
  });

  if (response.status === 401 && !isRetry && path !== "/auth/refresh" && path !== "/auth/login") {
    const refreshed = await refreshSession();
    if (refreshed) {
      return request<T>(path, options, true);
    }
  }

  const json = (await response.json().catch(() => null)) as ApiResponse<T> | null;

  if (!response.ok || !json || json.success === false) {
    const code = json && json.success === false ? json.error.code : "UNKNOWN_ERROR";
    const message =
      json && json.success === false ? json.error.message : `Request failed with status ${response.status}`;
    throw new ApiClientError(code, message, response.status);
  }

  return json.data;
}

export const apiClient = {
  get: <T>(path: string, options?: RequestOptions) => request<T>(path, { ...options, method: "GET" }),
  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "POST", body }),
  put: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "PUT", body }),
  patch: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "PATCH", body }),
  delete: <T>(path: string, options?: RequestOptions) => request<T>(path, { ...options, method: "DELETE" }),
  upload: <T>(path: string, formData: FormData) => request<T>(path, { method: "POST", body: formData }),
};
