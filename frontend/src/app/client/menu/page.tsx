import type { Metadata } from "next";

import { MenuEditorContent } from "@/features/menu/menu-editor-content";

export const metadata: Metadata = { title: "Menu" };

export default function ClientMenuPage() {
  return <MenuEditorContent />;
}
