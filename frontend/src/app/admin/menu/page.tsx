import type { Metadata } from "next";
import { Suspense } from "react";

import { AdminMenuPageContent } from "@/features/menu/admin-menu-page-content";

export const metadata: Metadata = { title: "Menus" };

export default function AdminMenuPage() {
  return (
    <Suspense fallback={null}>
      <AdminMenuPageContent />
    </Suspense>
  );
}
