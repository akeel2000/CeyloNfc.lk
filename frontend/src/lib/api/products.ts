import { apiClient } from "@/lib/api/client";
import type { Product } from "@/lib/types/commerce";
import type { ProductFormValues } from "@/lib/schemas/commerce";

export const productsApi = {
  list: () => apiClient.get<Product[]>("/admin/products"),
  create: (payload: ProductFormValues) => apiClient.post<Product>("/admin/products", payload),
  update: (uuid: string, payload: ProductFormValues) =>
    apiClient.put<Product>(`/admin/products/${uuid}`, payload),
};
