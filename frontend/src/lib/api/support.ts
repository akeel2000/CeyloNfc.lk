import { apiClient } from "@/lib/api/client";
import type { SupportTicket, TicketStatus } from "@/lib/types/support";
import type { PageResponse } from "@/lib/types/client";
import type { TicketCreateFormValues, MessageCreateFormValues } from "@/lib/schemas/support";

export interface SupportListParams {
  page?: number;
  size?: number;
  status?: string;
  search?: string;
}

function buildQuery(params: SupportListParams): string {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set("page", String(params.page));
  if (params.size !== undefined) query.set("size", String(params.size));
  if (params.status) query.set("status", params.status);
  if (params.search) query.set("search", params.search);
  const qs = query.toString();
  return qs ? `?${qs}` : "";
}

export const supportApi = {
  listOwn: () => apiClient.get<SupportTicket[]>("/client/support"),
  createOwn: (payload: TicketCreateFormValues & { attachmentUrl?: string }) =>
    apiClient.post<SupportTicket>("/client/support", payload),
  getOwn: (uuid: string) => apiClient.get<SupportTicket>(`/client/support/${uuid}`),
  replyOwn: (uuid: string, payload: MessageCreateFormValues & { attachmentUrl?: string }) =>
    apiClient.post<SupportTicket>(`/client/support/${uuid}/messages`, payload),

  listAdmin: (params: SupportListParams = {}) =>
    apiClient.get<PageResponse<SupportTicket>>(`/admin/support${buildQuery(params)}`),
  getAdmin: (uuid: string) => apiClient.get<SupportTicket>(`/admin/support/${uuid}`),
  replyAdmin: (uuid: string, payload: MessageCreateFormValues & { attachmentUrl?: string }) =>
    apiClient.post<SupportTicket>(`/admin/support/${uuid}/messages`, payload),
  updateStatus: (uuid: string, status: TicketStatus) =>
    apiClient.patch<SupportTicket>(`/admin/support/${uuid}/status?status=${status}`),
};
