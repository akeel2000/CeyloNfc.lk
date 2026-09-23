import type { Metadata } from "next";

import { AdminSupportListContent } from "@/features/support/admin-support-list-content";

export const metadata: Metadata = { title: "Support" };

export default function AdminSupportPage() {
  return <AdminSupportListContent />;
}
