import type { Metadata } from "next";

import { ClientOrdersPageContent } from "@/features/orders/client-orders-page-content";

export const metadata: Metadata = { title: "Orders" };

export default function ClientOrdersPage() {
  return <ClientOrdersPageContent />;
}
