import type { Metadata } from "next";

import { QrCodesPageContent } from "@/features/qr/qr-codes-page-content";

export const metadata: Metadata = { title: "QR Codes" };

export default function ClientQrCodesPage() {
  return <QrCodesPageContent />;
}
