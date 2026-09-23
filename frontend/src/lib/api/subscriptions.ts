import { apiClient } from "@/lib/api/client";
import type { Subscription } from "@/lib/types/commerce";
import type { PageResponse } from "@/lib/types/client";
import type { SubscriptionAssignFormValues } from "@/lib/schemas/commerce";

export interface SubscriptionListParams {
  page?: number;
  size?: number;
  status?: string;
  packagePlanUuid?: string;
}

function buildQuery(params: SubscriptionListParams): string {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set("page", String(params.page));
  if (params.size !== undefined) query.set("size", String(params.size));
  if (params.status) query.set("status", params.status);
  if (params.packagePlanUuid) query.set("packagePlanUuid", params.packagePlanUuid);
  const qs = query.toString();
  return qs ? `?${qs}` : "";
}

export const subscriptionsApi = {
  getForClient: (clientUuid: string) =>
    apiClient.get<Subscription>(`/admin/subscriptions?clientUuid=${clientUuid}`),
  list: (params: SubscriptionListParams = {}) =>
    apiClient.get<PageResponse<Subscription>>(`/admin/subscriptions/list${buildQuery(params)}`),
  assign: (clientUuid: string, payload: SubscriptionAssignFormValues) =>
    apiClient.post<Subscription>(`/admin/subscriptions?clientUuid=${clientUuid}`, payload),
  getOwn: () => apiClient.get<Subscription>("/client/subscription"),
};
