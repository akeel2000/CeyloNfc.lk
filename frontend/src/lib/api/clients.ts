import { apiClient } from "@/lib/api/client";
import type { Client, ClientCreateResult, PageResponse } from "@/lib/types/client";
import type { ClientCreateFormValues, ClientUpdateFormValues } from "@/lib/schemas/client";

export interface ClientListParams {
  page?: number;
  size?: number;
  search?: string;
  status?: string;
  type?: string;
}

function buildQuery(params: ClientListParams): string {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set("page", String(params.page));
  if (params.size !== undefined) query.set("size", String(params.size));
  if (params.search) query.set("search", params.search);
  if (params.status) query.set("status", params.status);
  if (params.type) query.set("type", params.type);
  const qs = query.toString();
  return qs ? `?${qs}` : "";
}

export const clientsApi = {
  list: (params: ClientListParams = {}) =>
    apiClient.get<PageResponse<Client>>(`/admin/clients${buildQuery(params)}`),
  get: (uuid: string) => apiClient.get<Client>(`/admin/clients/${uuid}`),
  create: (payload: ClientCreateFormValues) =>
    apiClient.post<ClientCreateResult>("/admin/clients", payload),
  update: (uuid: string, payload: ClientUpdateFormValues) =>
    apiClient.put<Client>(`/admin/clients/${uuid}`, payload),
  updateStatus: (uuid: string, status: string) =>
    apiClient.patch<Client>(`/admin/clients/${uuid}/status`, { status }),
  delete: (uuid: string) => apiClient.delete<void>(`/admin/clients/${uuid}`),
};
