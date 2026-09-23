import { apiClient } from "@/lib/api/client";
import type { GoogleReviewLocation } from "@/lib/types/review";
import type { ReviewLocationFormValues } from "@/lib/schemas/review";

export const reviewLocationsApi = {
  listOwn: () => apiClient.get<GoogleReviewLocation[]>("/client/google-reviews"),
  create: (payload: ReviewLocationFormValues) =>
    apiClient.post<GoogleReviewLocation>("/client/google-reviews", payload),
  update: (uuid: string, payload: ReviewLocationFormValues) =>
    apiClient.put<GoogleReviewLocation>(`/client/google-reviews/${uuid}`, payload),
};

export const adminReviewLocationsApi = {
  listForClient: (clientUuid: string) =>
    apiClient.get<GoogleReviewLocation[]>(`/admin/google-reviews?clientUuid=${clientUuid}`),
  create: (clientUuid: string, payload: ReviewLocationFormValues) =>
    apiClient.post<GoogleReviewLocation>(`/admin/google-reviews?clientUuid=${clientUuid}`, payload),
  update: (uuid: string, payload: ReviewLocationFormValues) =>
    apiClient.put<GoogleReviewLocation>(`/admin/google-reviews/${uuid}`, payload),
  count: () => apiClient.get<number>("/admin/google-reviews/count"),
};
