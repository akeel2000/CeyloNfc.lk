import type { Metadata } from "next";

import { AnalyticsPageContent } from "@/features/analytics/analytics-page-content";

export const metadata: Metadata = { title: "Analytics" };

export default function ClientAnalyticsPage() {
  return <AnalyticsPageContent />;
}
