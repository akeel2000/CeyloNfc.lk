import type { Metadata } from "next";

import { LeadsPageContent } from "@/features/leads/leads-page-content";

export const metadata: Metadata = { title: "Leads" };

export default function AdminLeadsPage() {
  return <LeadsPageContent />;
}
