"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Nfc, QrCode, Eye } from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { StatCard } from "@/components/ui/stat-card";
import { PageHeader } from "@/components/layout/page-header";
import { useAuth } from "@/lib/providers/auth-provider";
import { clientNfcCardsApi } from "@/lib/api/nfc";
import { analyticsApi } from "@/lib/api/analytics";
import { NfcStatusBadge } from "@/features/nfc/nfc-status-badge";

export default function ClientDashboardPage() {
  const { user } = useAuth();
  const { data: cards, isLoading } = useQuery({
    queryKey: ["client", "nfc-cards"],
    queryFn: () => clientNfcCardsApi.list(),
  });
  const { data: analytics, isLoading: isAnalyticsLoading } = useQuery({
    queryKey: ["client", "analytics", "summary", 7],
    queryFn: () => analyticsApi.clientSummary(7),
  });

  const activeCards = cards?.filter((c) => c.status === "ACTIVE").length;
  const totalTaps = cards?.reduce((sum, c) => sum + c.totalTaps, 0);

  return (
    <div className="space-y-6">
      <PageHeader
        title={`Welcome${user ? `, ${user.email}` : ""}`}
        description="Your NFC cards, destinations and analytics."
      />

      <div className="grid gap-4 sm:grid-cols-3">
        <StatCard
          label="Active NFC Cards"
          value={isLoading ? "—" : activeCards ?? "—"}
          icon={Nfc}
          color="text-primary bg-primary/10"
          hint={isLoading ? "No data yet" : "Live"}
        />
        <StatCard
          label="Total Taps"
          value={isLoading ? "—" : totalTaps ?? "—"}
          icon={QrCode}
          color="text-primary bg-primary/10"
          hint={isLoading ? "No data yet" : "Live"}
        />
        <StatCard
          label="Profile Views"
          value={isAnalyticsLoading ? "—" : analytics?.totalProfileViews ?? "—"}
          icon={Eye}
          color="text-primary bg-primary/10"
          hint={isAnalyticsLoading ? "No data yet" : "Live"}
        />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>{cards && cards.length > 0 ? "Your NFC cards" : "No NFC cards yet"}</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          {cards && cards.length > 0 ? (
            <ul className="divide-y divide-border">
              {cards.map((card) => (
                <li key={card.uuid} className="flex items-center justify-between py-2">
                  <span className="font-medium text-foreground">{card.serialNumber}</span>
                  <NfcStatusBadge status={card.status} />
                </li>
              ))}
            </ul>
          ) : (
            <p>
              Your assigned NFC cards will appear here once an admin registers and assigns one
              to your account. See{" "}
              <Link href="/client/nfc-cards" className="underline">
                NFC Cards
              </Link>
              .
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
