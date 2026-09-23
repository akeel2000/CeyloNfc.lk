import type { Metadata } from "next";

import { AdminAnalyticsPageContent } from "@/features/analytics/admin-analytics-page-content";

export const metadata: Metadata = { title: "Analytics" };

export default function AdminAnalyticsPage() {
  return <AdminAnalyticsPageContent />;
}
