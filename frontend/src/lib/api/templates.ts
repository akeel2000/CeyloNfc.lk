import { apiClient } from "@/lib/api/client";
import type { Profile } from "@/lib/types/profile";
import type { TemplateFormValues } from "@/lib/schemas/template";
import type { Template, TemplateGalleryItem } from "@/lib/types/template";

export const templatesApi = {
  listAdmin: () => apiClient.get<Template[]>("/admin/templates"),
  create: (payload: TemplateFormValues) => apiClient.post<Template>("/admin/templates", payload),
  update: (uuid: string, payload: TemplateFormValues) =>
    apiClient.put<Template>(`/admin/templates/${uuid}`, payload),
  gallery: () => apiClient.get<TemplateGalleryItem[]>("/client/templates"),
  apply: (templateUuid: string) => apiClient.post<Profile>(`/client/profile/template/${templateUuid}`),
};
