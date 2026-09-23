import type { Metadata } from "next";

import { AdminSettingsPageContent } from "@/features/settings/admin-settings-page-content";

export const metadata: Metadata = { title: "Settings" };

export default function AdminSettingsPage() {
  return <AdminSettingsPageContent />;
}
