import type { Metadata } from "next";

import { SubscriptionsPageContent } from "@/features/subscriptions/subscriptions-page-content";

export const metadata: Metadata = { title: "Subscriptions" };

export default function AdminSubscriptionsPage() {
  return <SubscriptionsPageContent />;
}
