import { apiClient } from "@/lib/api/client";
import type { Notification } from "@/lib/types/notification";
import type { PageResponse } from "@/lib/types/client";

export const notificationsApi = {
  list: () => apiClient.get<PageResponse<Notification>>("/notifications?size=10"),
  unreadCount: () => apiClient.get<{ count: number }>("/notifications/unread-count"),
  markRead: (uuid: string) => apiClient.post<Notification>(`/notifications/${uuid}/read`),
  markAllRead: () => apiClient.post<void>("/notifications/read-all"),
};
