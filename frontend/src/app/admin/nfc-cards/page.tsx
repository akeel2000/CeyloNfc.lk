import type { Metadata } from "next";

import { NfcCardsPageContent } from "@/features/nfc/nfc-cards-page-content";

export const metadata: Metadata = { title: "NFC Cards" };

export default function AdminNfcCardsPage() {
  return <NfcCardsPageContent />;
}
