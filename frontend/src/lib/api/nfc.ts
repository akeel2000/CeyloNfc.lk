import { apiClient } from "@/lib/api/client";
import type { Destination, NfcCard, NfcCardRegisterResult } from "@/lib/types/nfc";
import type { PageResponse } from "@/lib/types/client";
import type { NfcCardRegisterFormValues, NfcCardReplaceFormValues } from "@/lib/schemas/nfc";

export interface NfcCardListParams {
  page?: number;
  size?: number;
  search?: string;
  status?: string;
  clientUuid?: string;
}

function buildQuery(params: Record<string, string | number | undefined>): string {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== "") query.set(key, String(value));
  });
  const qs = query.toString();
  return qs ? `?${qs}` : "";
}

export const nfcCardsApi = {
  list: (params: NfcCardListParams = {}) =>
    apiClient.get<PageResponse<NfcCard>>(`/admin/nfc-cards${buildQuery({ ...params })}`),
  get: (uuid: string) => apiClient.get<NfcCard>(`/admin/nfc-cards/${uuid}`),
  register: (payload: NfcCardRegisterFormValues) =>
    apiClient.post<NfcCardRegisterResult>("/admin/nfc-cards", payload),
  replace: (uuid: string, payload: NfcCardReplaceFormValues) =>
    apiClient.post<NfcCardRegisterResult>(`/admin/nfc-cards/${uuid}/replace`, payload),
  assign: (uuid: string, clientUuid: string, destinationUuid: string) =>
    apiClient.post<NfcCard>(`/admin/nfc-cards/${uuid}/assign`, { clientUuid, destinationUuid }),
  activate: (uuid: string) => apiClient.post<NfcCard>(`/admin/nfc-cards/${uuid}/activate`),
  suspend: (uuid: string) => apiClient.post<NfcCard>(`/admin/nfc-cards/${uuid}/suspend`),
};

export const destinationsApi = {
  listForClient: (clientUuid: string) =>
    apiClient.get<Destination[]>(`/admin/destinations${buildQuery({ clientUuid })}`),
  create: (payload: {
    clientUuid: string;
    name: string;
    type: string;
    externalUrl?: string;
    googleReviewLocationUuid?: string;
  }) => apiClient.post<Destination>("/admin/destinations", payload),
};

export const clientNfcCardsApi = {
  list: () => apiClient.get<NfcCard[]>("/client/nfc-cards"),
  get: (uuid: string) => apiClient.get<NfcCard>(`/client/nfc-cards/${uuid}`),
};
