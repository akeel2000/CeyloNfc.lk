import { apiClient } from "@/lib/api/client";
import type { Menu } from "@/lib/types/menu";
import type { MenuUpdateFormValues, MenuCategoryFormValues, MenuItemFormValues } from "@/lib/schemas/menu";

export const menuApi = {
  getOwn: () => apiClient.get<Menu>("/client/menu"),
  update: (payload: MenuUpdateFormValues) => apiClient.put<Menu>("/client/menu", payload),
  setPublished: (published: boolean) => apiClient.post<Menu>("/client/menu/publish", { published }),

  createCategory: (payload: MenuCategoryFormValues) =>
    apiClient.post<Menu>("/client/menu/categories", payload),
  updateCategory: (categoryUuid: string, payload: MenuCategoryFormValues & { active?: boolean; sortOrder?: number }) =>
    apiClient.put<Menu>(`/client/menu/categories/${categoryUuid}`, payload),
  deleteCategory: (categoryUuid: string) =>
    apiClient.delete<Menu>(`/client/menu/categories/${categoryUuid}`),

  createItem: (categoryUuid: string, payload: MenuItemFormValues) =>
    apiClient.post<Menu>(`/client/menu/categories/${categoryUuid}/items`, payload),
  updateItem: (categoryUuid: string, itemUuid: string, payload: MenuItemFormValues & { sortOrder?: number }) =>
    apiClient.put<Menu>(`/client/menu/categories/${categoryUuid}/items/${itemUuid}`, payload),
  deleteItem: (categoryUuid: string, itemUuid: string) =>
    apiClient.delete<Menu>(`/client/menu/categories/${categoryUuid}/items/${itemUuid}`),
};

export const adminMenuApi = {
  get: (clientUuid: string) => apiClient.get<Menu>(`/admin/menu?clientUuid=${clientUuid}`),
  update: (clientUuid: string, payload: MenuUpdateFormValues) =>
    apiClient.put<Menu>(`/admin/menu?clientUuid=${clientUuid}`, payload),
  setPublished: (clientUuid: string, published: boolean) =>
    apiClient.post<Menu>(`/admin/menu/publish?clientUuid=${clientUuid}`, { published }),

  createCategory: (clientUuid: string, payload: MenuCategoryFormValues) =>
    apiClient.post<Menu>(`/admin/menu/categories?clientUuid=${clientUuid}`, payload),
  updateCategory: (
    clientUuid: string,
    categoryUuid: string,
    payload: MenuCategoryFormValues & { active?: boolean; sortOrder?: number }
  ) => apiClient.put<Menu>(`/admin/menu/categories/${categoryUuid}?clientUuid=${clientUuid}`, payload),
  deleteCategory: (clientUuid: string, categoryUuid: string) =>
    apiClient.delete<Menu>(`/admin/menu/categories/${categoryUuid}?clientUuid=${clientUuid}`),

  createItem: (clientUuid: string, categoryUuid: string, payload: MenuItemFormValues) =>
    apiClient.post<Menu>(`/admin/menu/categories/${categoryUuid}/items?clientUuid=${clientUuid}`, payload),
  updateItem: (
    clientUuid: string,
    categoryUuid: string,
    itemUuid: string,
    payload: MenuItemFormValues & { sortOrder?: number }
  ) =>
    apiClient.put<Menu>(
      `/admin/menu/categories/${categoryUuid}/items/${itemUuid}?clientUuid=${clientUuid}`,
      payload
    ),
  deleteItem: (clientUuid: string, categoryUuid: string, itemUuid: string) =>
    apiClient.delete<Menu>(`/admin/menu/categories/${categoryUuid}/items/${itemUuid}?clientUuid=${clientUuid}`),
};

export const publicMenuApi = {
  get: (slug: string) => apiClient.get<Menu>(`/public/menu/${slug}`),
};
