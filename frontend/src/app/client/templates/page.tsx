import type { Metadata } from "next";

import { TemplateGallery } from "@/features/templates/template-gallery";

export const metadata: Metadata = { title: "Templates" };

export default function ClientTemplatesPage() {
  return <TemplateGallery />;
}
