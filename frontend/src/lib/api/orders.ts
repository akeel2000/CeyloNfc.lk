import { apiClient } from "@/lib/api/client";
import type { Order, OrderStatus, PaymentStatus } from "@/lib/types/commerce";
import type { PageResponse } from "@/lib/types/client";
import type { OrderCreateFormValues } from "@/lib/schemas/commerce";

export interface OrderListParams {
  page?: number;
  size?: number;
  status?: string;
  search?: string;
}

function buildQuery(params: OrderListParams): string {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set("page", String(params.page));
  if (params.size !== undefined) query.set("size", String(params.size));
  if (params.status) query.set("status", params.status);
  if (params.search) query.set("search", params.search);
  const qs = query.toString();
  return qs ? `?${qs}` : "";
}

export const ordersApi = {
  list: (params: OrderListParams = {}) =>
    apiClient.get<PageResponse<Order>>(`/admin/orders${buildQuery(params)}`),
  get: (uuid: string) => apiClient.get<Order>(`/admin/orders/${uuid}`),
  create: (payload: OrderCreateFormValues) => apiClient.post<Order>("/admin/orders", payload),
  updateStatus: (uuid: string, status: OrderStatus) =>
    apiClient.patch<Order>(`/admin/orders/${uuid}/status?status=${status}`),
  updatePaymentStatus: (uuid: string, paymentStatus: PaymentStatus) =>
    apiClient.patch<Order>(`/admin/orders/${uuid}/payment-status?paymentStatus=${paymentStatus}`),
  listOwn: () => apiClient.get<Order[]>("/client/orders"),
};
