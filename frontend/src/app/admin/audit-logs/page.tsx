import type { Metadata } from "next";

import { AuditLogsPageContent } from "@/features/audit/audit-logs-page-content";

export const metadata: Metadata = { title: "Audit Logs" };

export default function AdminAuditLogsPage() {
  return <AuditLogsPageContent />;
}
