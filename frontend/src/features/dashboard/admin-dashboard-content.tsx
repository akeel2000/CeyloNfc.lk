"use client";

import { useQuery } from "@tanstack/react-query";
import { Users, Nfc, QrCode, Star } from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { StatCard } from "@/components/ui/stat-card";
import { PageHeader } from "@/components/layout/page-header";
import { clientsApi } from "@/lib/api/clients";
import { nfcCardsApi } from "@/lib/api/nfc";
import { adminQrCodesApi } from "@/lib/api/qr";
import { adminReviewLocationsApi } from "@/lib/api/review";

const KPIS = [
  { key: "clients", label: "Total Clients", icon: Users, color: "text-primary bg-primary/10" },
  { key: "cards", label: "Active NFC Cards", icon: Nfc, color: "text-primary bg-primary/10" },
  { key: "qr", label: "QR Codes", icon: QrCode, color: "text-primary bg-primary/10" },
  { key: "reviews", label: "Google Review Locations", icon: Star, color: "text-primary bg-primary/10" },
] as const;

export function AdminDashboardContent() {
  const { data: clientsPage } = useQuery({
    queryKey: ["clients", { size: 1, forCount: true }],
    queryFn: () => clientsApi.list({ size: 1 }),
  });

  const { data: activeCardsPage } = useQuery({
    queryKey: ["nfc-cards", { size: 1, status: "ACTIVE", forCount: true }],
    queryFn: () => nfcCardsApi.list({ size: 1, status: "ACTIVE" }),
  });

  const { data: qrCount } = useQuery({
    queryKey: ["admin", "qr-codes", "count"],
    queryFn: () => adminQrCodesApi.count(),
  });

  const { data: reviewLocationCount } = useQuery({
    queryKey: ["admin", "google-reviews", "count"],
    queryFn: () => adminReviewLocationsApi.count(),
  });

  const values: Record<string, number | undefined> = {
    clients: clientsPage?.totalElements,
    cards: activeCardsPage?.totalElements,
    qr: qrCount,
    reviews: reviewLocationCount,
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Dashboard"
        description="Platform overview. Metrics populate as each module connects."
      />

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {KPIS.map((kpi) => {
          const value = values[kpi.key];
          return (
            <StatCard
              key={kpi.label}
              label={kpi.label}
              value={value ?? "—"}
              icon={kpi.icon}
              color={kpi.color}
              hint={value === undefined ? "No data yet" : "Live"}
            />
          );
        })}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Getting started</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          <p>
            Client management, NFC card registration/redirect, QR codes, Google Review
            locations, and the profile template gallery are all live end-to-end. See{" "}
            <code>docs/PROJECT_PROGRESS.md</code> for what&apos;s still pending.
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
