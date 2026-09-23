import type { Metadata } from "next";

import { AdminUserDetailContent } from "@/features/admin-users/admin-user-detail-content";

export const metadata: Metadata = { title: "User Detail" };

export default async function AdminUserDetailPage({
  params,
}: {
  params: Promise<{ uuid: string }>;
}) {
  const { uuid } = await params;
  return <AdminUserDetailContent uuid={uuid} />;
}
