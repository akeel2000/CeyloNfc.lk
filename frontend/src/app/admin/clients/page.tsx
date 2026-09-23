import type { Metadata } from "next";

import { ClientsPageContent } from "@/features/clients/clients-page-content";

export const metadata: Metadata = { title: "Clients" };

export default function AdminClientsPage() {
  return <ClientsPageContent />;
}
