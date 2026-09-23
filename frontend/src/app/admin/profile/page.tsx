import type { Metadata } from "next";
import { Suspense } from "react";

import { AdminProfilePageContent } from "@/features/profile/admin-profile-page-content";

export const metadata: Metadata = { title: "Profiles" };

export default function AdminProfilePage() {
  return (
    <Suspense fallback={null}>
      <AdminProfilePageContent />
    </Suspense>
  );
}
