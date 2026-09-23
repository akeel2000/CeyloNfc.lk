import type { Metadata } from "next";
import { Suspense } from "react";
import { CircleAlert, Clock, Search } from "lucide-react";

import { brand } from "@/lib/config/brand";

export const metadata: Metadata = { title: "Card unavailable" };

const REASONS: Record<string, { icon: typeof CircleAlert; title: string; description: string }> = {
  inactive: {
    icon: CircleAlert,
    title: "This card is not active",
    description:
      "This NFC card has been suspended or hasn't been activated yet. If you're the owner, contact support to reactivate it.",
  },
  "rate-limited": {
    icon: Clock,
    title: "Too many attempts",
    description: "Please wait a moment and tap the card again.",
  },
  "not-found": {
    icon: Search,
    title: "Card not recognized",
    description: "This link doesn't match a registered card. Double-check the card and try again.",
  },
};

async function CardUnavailableContent({
  searchParams,
}: {
  searchParams: Promise<{ reason?: string }>;
}) {
  const { reason } = await searchParams;
  const info = REASONS[reason ?? "not-found"] ?? REASONS["not-found"];

  return (
    <div className="flex min-h-full flex-1 flex-col items-center justify-center gap-4 px-4 py-24 text-center">
      <span className="flex size-14 items-center justify-center rounded-full bg-secondary">
        <info.icon className="size-6 text-muted-foreground" />
      </span>
      <h1 className="text-2xl font-semibold tracking-tight">{info.title}</h1>
      <p className="max-w-sm text-sm text-muted-foreground">{info.description}</p>
      <p className="mt-4 text-xs text-muted-foreground">{brand.name}</p>
    </div>
  );
}

export default function CardUnavailablePage({
  searchParams,
}: {
  searchParams: Promise<{ reason?: string }>;
}) {
  return (
    <Suspense fallback={null}>
      <CardUnavailableContent searchParams={searchParams} />
    </Suspense>
  );
}
