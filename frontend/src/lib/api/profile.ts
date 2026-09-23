import { apiClient } from "@/lib/api/client";
import type { BusinessHour, Profile, PublicProfile, SocialLink } from "@/lib/types/profile";
import type { ProfileUpdateFormValues } from "@/lib/schemas/profile";

export const profileApi = {
  getOwn: () => apiClient.get<Profile>("/client/profile"),
  update: (payload: ProfileUpdateFormValues) => apiClient.put<Profile>("/client/profile", payload),
  setPublished: (published: boolean) => apiClient.post<Profile>("/client/profile/publish", { published }),
  updateSocialLinks: (links: SocialLink[]) => apiClient.put<SocialLink[]>("/client/profile/social-links", links),
  updateBusinessHours: (hours: BusinessHour[]) =>
    apiClient.put<BusinessHour[]>("/client/profile/business-hours", hours),
};

export const adminProfileApi = {
  get: (clientUuid: string) => apiClient.get<Profile>(`/admin/profile?clientUuid=${clientUuid}`),
  update: (clientUuid: string, payload: ProfileUpdateFormValues) =>
    apiClient.put<Profile>(`/admin/profile?clientUuid=${clientUuid}`, payload),
  setPublished: (clientUuid: string, published: boolean) =>
    apiClient.post<Profile>(`/admin/profile/publish?clientUuid=${clientUuid}`, { published }),
  updateSocialLinks: (clientUuid: string, links: SocialLink[]) =>
    apiClient.put<SocialLink[]>(`/admin/profile/social-links?clientUuid=${clientUuid}`, links),
  updateBusinessHours: (clientUuid: string, hours: BusinessHour[]) =>
    apiClient.put<BusinessHour[]>(`/admin/profile/business-hours?clientUuid=${clientUuid}`, hours),
};

export const publicProfileApi = {
  individual: (slug: string) => apiClient.get<PublicProfile>(`/public/profile/${slug}`),
  company: (slug: string) => apiClient.get<PublicProfile>(`/public/company/${slug}`),
};
