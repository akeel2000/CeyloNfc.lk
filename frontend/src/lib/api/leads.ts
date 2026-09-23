import { apiClient } from "@/lib/api/client";
import type { Lead, LeadConvertResult } from "@/lib/types/lead";
import type { PageResponse } from "@/lib/types/client";
import type { LeadCreateFormValues, LeadUpdateFormValues } from "@/lib/schemas/lead";

export interface LeadListParams {
  page?: number;
  size?: number;
  status?: string;
  search?: string;
}

function buildQuery(params: LeadListParams): string {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set("page", String(params.page));
  if (params.size !== undefined) query.set("size", String(params.size));
  if (params.status) query.set("status", params.status);
  if (params.search) query.set("search", params.search);
  const qs = query.toString();
  return qs ? `?${qs}` : "";
}

export const leadsApi = {
  createPublic: (payload: LeadCreateFormValues) =>
    apiClient.post<Lead>("/public/leads", { ...payload, source: "website" }),
  list: (params: LeadListParams = {}) => apiClient.get<PageResponse<Lead>>(`/admin/leads${buildQuery(params)}`),
  update: (uuid: string, payload: LeadUpdateFormValues) =>
    apiClient.patch<Lead>(`/admin/leads/${uuid}`, payload),
  convert: (uuid: string, clientType: "INDIVIDUAL" | "BUSINESS") =>
    apiClient.post<LeadConvertResult>(`/admin/leads/${uuid}/convert`, { clientType }),
};
