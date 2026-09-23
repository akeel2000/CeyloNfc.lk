import type { Metadata } from "next";

import { AdminQrCodesPageContent } from "@/features/qr/admin-qr-codes-page-content";

export const metadata: Metadata = { title: "QR Codes" };

export default function AdminQrCodesPage() {
  return <AdminQrCodesPageContent />;
}
