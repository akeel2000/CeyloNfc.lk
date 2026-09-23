import type { Metadata } from "next";

import { ClientDetailContent } from "@/features/clients/client-detail-content";

export const metadata: Metadata = { title: "Client Detail" };

export default async function AdminClientDetailPage({
  params,
}: {
  params: Promise<{ uuid: string }>;
}) {
  const { uuid } = await params;
  return <ClientDetailContent uuid={uuid} />;
}
