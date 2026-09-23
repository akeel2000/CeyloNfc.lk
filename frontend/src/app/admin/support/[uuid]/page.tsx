import type { Metadata } from "next";

import { AdminSupportDetailContent } from "@/features/support/admin-support-detail-content";

export const metadata: Metadata = { title: "Support Ticket" };

export default async function AdminSupportDetailPage({
  params,
}: {
  params: Promise<{ uuid: string }>;
}) {
  const { uuid } = await params;
  return <AdminSupportDetailContent uuid={uuid} />;
}
