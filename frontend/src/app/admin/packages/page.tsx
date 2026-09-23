import type { Metadata } from "next";

import { PackagesPageContent } from "@/features/packages/packages-page-content";

export const metadata: Metadata = { title: "Packages" };

export default function AdminPackagesPage() {
  return <PackagesPageContent />;
}
