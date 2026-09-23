import { apiClient } from "@/lib/api/client";
import type { AnalyticsSummary } from "@/lib/types/analytics";

export const analyticsApi = {
  clientSummary: (days: number) => apiClient.get<AnalyticsSummary>(`/client/analytics/summary?days=${days}`),
  adminSummary: (clientUuid: string, days: number) =>
    apiClient.get<AnalyticsSummary>(`/admin/analytics/summary?clientUuid=${clientUuid}&days=${days}`),
};
