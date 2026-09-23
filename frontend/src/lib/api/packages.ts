import { apiClient } from "@/lib/api/client";
import type { PackagePlan } from "@/lib/types/commerce";
import type { PackagePlanPayload } from "@/lib/schemas/commerce";

export const packagesApi = {
  listAdmin: () => apiClient.get<PackagePlan[]>("/admin/packages"),
  listPublic: () => apiClient.get<PackagePlan[]>("/public/packages"),
  create: (payload: PackagePlanPayload) => apiClient.post<PackagePlan>("/admin/packages", payload),
  update: (uuid: string, payload: PackagePlanPayload) =>
    apiClient.put<PackagePlan>(`/admin/packages/${uuid}`, payload),
};
