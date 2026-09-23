import type { Metadata } from "next";

import { ReviewsPageContent } from "@/features/reviews/reviews-page-content";

export const metadata: Metadata = { title: "Google Reviews" };

export default function ClientGoogleReviewsPage() {
  return <ReviewsPageContent />;
}
