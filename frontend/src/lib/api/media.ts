import { apiClient } from "@/lib/api/client";

export type MediaCategory =
  | "PROFILE_PHOTO"
  | "PROFILE_COVER"
  | "COMPANY_LOGO"
  | "MENU_ITEM"
  | "TICKET_ATTACHMENT"
  | "TEMPLATE_PREVIEW";

export const mediaApi = {
  upload: (file: File, category: MediaCategory) => {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("category", category);
    return apiClient.upload<{ url: string }>("/client/media/upload", formData);
  },
};
