import type { Metadata } from "next";

import { ClientNfcCardsContent } from "@/features/nfc/client-nfc-cards-content";

export const metadata: Metadata = { title: "My NFC Cards" };

export default function ClientNfcCardsPage() {
  return <ClientNfcCardsContent />;
}
