import { apiClient } from "@/lib/api/client";
import type { QrCode, QrCodeCreateResult } from "@/lib/types/qr";
import type { QrCreateFormValues } from "@/lib/schemas/qr";

export const qrCodesApi = {
  listOwn: () => apiClient.get<QrCode[]>("/client/qr-codes"),
  create: (payload: QrCreateFormValues) => apiClient.post<QrCodeCreateResult>("/client/qr-codes", payload),
  setStatus: (uuid: string, active: boolean) =>
    apiClient.patch<QrCode>(`/client/qr-codes/${uuid}/status`, { active }),
};

export const adminQrCodesApi = {
  listForClient: (clientUuid: string) => apiClient.get<QrCode[]>(`/admin/qr-codes?clientUuid=${clientUuid}`),
  create: (clientUuid: string, payload: QrCreateFormValues) =>
    apiClient.post<QrCodeCreateResult>(`/admin/qr-codes?clientUuid=${clientUuid}`, payload),
  setStatus: (uuid: string, active: boolean) =>
    apiClient.patch<QrCode>(`/admin/qr-codes/${uuid}/status`, { active }),
  count: () => apiClient.get<number>("/admin/qr-codes/count"),
};
