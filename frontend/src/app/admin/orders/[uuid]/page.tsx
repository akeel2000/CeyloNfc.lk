import type { Metadata } from "next";

import { OrderDetailContent } from "@/features/orders/order-detail-content";

export const metadata: Metadata = { title: "Order Detail" };

export default async function AdminOrderDetailPage({
  params,
}: {
  params: Promise<{ uuid: string }>;
}) {
  const { uuid } = await params;
  return <OrderDetailContent uuid={uuid} />;
}
