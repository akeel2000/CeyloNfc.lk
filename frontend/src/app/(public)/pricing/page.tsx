import type { Metadata } from "next";

import { PricingContent } from "@/features/packages/pricing-content";
import { brand } from "@/lib/config/brand";

export const metadata: Metadata = {
  title: "Pricing",
  description: `Compare ${brand.name} plans and pick the one that fits your business.`,
};

export default function PricingPage() {
  return (
    <div className="relative">
      <div className="pointer-events-none absolute inset-0 -z-10 bg-[radial-gradient(circle_at_top,_var(--secondary)_0%,_transparent_60%)]" />
      <div className="mx-auto max-w-6xl px-4 py-20 sm:px-6">
        <div className="mx-auto max-w-2xl text-center">
          <h1 className="text-3xl font-semibold tracking-tight sm:text-5xl">Simple, transparent pricing</h1>
          <p className="mt-4 text-muted-foreground">
            Every plan includes a permanent, secure NFC link and QR code. Upgrade any time as your
            business grows.
          </p>
        </div>
        <PricingContent />
      </div>
    </div>
  );
}
