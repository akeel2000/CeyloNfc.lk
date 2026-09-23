import type { Metadata } from "next";

import { WriteNfcPageContent } from "@/features/nfc/write-nfc-page-content";

export const metadata: Metadata = { title: "Write NFC" };

export default function WriteNfcPage() {
  return <WriteNfcPageContent />;
}
