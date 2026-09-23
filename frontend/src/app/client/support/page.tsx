import type { Metadata } from "next";

import { ClientSupportListContent } from "@/features/support/client-support-list-content";

export const metadata: Metadata = { title: "Support" };

export default function ClientSupportPage() {
  return <ClientSupportListContent />;
}
