import type { Metadata } from "next";

import { OrdersPageContent } from "@/features/orders/orders-page-content";

export const metadata: Metadata = { title: "Orders" };

export default function AdminOrdersPage() {
  return <OrdersPageContent />;
}
