import type { Metadata } from "next";

import { ClientSupportDetailContent } from "@/features/support/client-support-detail-content";

export const metadata: Metadata = { title: "Support Ticket" };

export default async function ClientSupportDetailPage({
  params,
}: {
  params: Promise<{ uuid: string }>;
}) {
  const { uuid } = await params;
  return <ClientSupportDetailContent uuid={uuid} />;
}
