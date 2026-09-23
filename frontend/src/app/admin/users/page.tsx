import type { Metadata } from "next";

import { AdminUsersPageContent } from "@/features/admin-users/admin-users-page-content";

export const metadata: Metadata = { title: "Users" };

export default function AdminUsersPage() {
  return <AdminUsersPageContent />;
}
