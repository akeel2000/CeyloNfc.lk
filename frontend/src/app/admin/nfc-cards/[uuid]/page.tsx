import type { Metadata } from "next";

import { NfcCardDetailContent } from "@/features/nfc/nfc-card-detail-content";

export const metadata: Metadata = { title: "NFC Card Detail" };

export default async function AdminNfcCardDetailPage({
  params,
}: {
  params: Promise<{ uuid: string }>;
}) {
  const { uuid } = await params;
  return <NfcCardDetailContent uuid={uuid} />;
}
