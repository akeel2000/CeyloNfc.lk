import type { Metadata } from "next";

import { TemplatesPageContent } from "@/features/templates/templates-page-content";

export const metadata: Metadata = { title: "Templates" };

export default function AdminTemplatesPage() {
  return <TemplatesPageContent />;
}
