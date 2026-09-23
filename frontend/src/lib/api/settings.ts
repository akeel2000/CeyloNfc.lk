import { apiClient } from "@/lib/api/client";
import type { PlatformSettingsFormValues } from "@/lib/schemas/settings";
import type { PlatformSettings } from "@/lib/types/settings";

export const adminSettingsApi = {
  get: () => apiClient.get<PlatformSettings>("/admin/settings"),
  update: (payload: PlatformSettingsFormValues) => apiClient.put<PlatformSettings>("/admin/settings", payload),
};

export const publicSettingsApi = {
  get: () => apiClient.get<PlatformSettings>("/public/settings"),
};
