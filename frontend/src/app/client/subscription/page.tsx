import type { Metadata } from "next";

import { ClientSubscriptionContent } from "@/features/subscriptions/client-subscription-content";

export const metadata: Metadata = { title: "Subscription" };

export default function ClientSubscriptionPage() {
  return <ClientSubscriptionContent />;
}
