import type { Metadata } from "next";

import { AdminReviewsPageContent } from "@/features/reviews/admin-reviews-page-content";

export const metadata: Metadata = { title: "Google Reviews" };

export default function AdminGoogleReviewsPage() {
  return <AdminReviewsPageContent />;
}
