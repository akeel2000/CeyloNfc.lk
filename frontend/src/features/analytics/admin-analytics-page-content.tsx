"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Nfc, QrCode, Eye, Trophy, BarChart3 } from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { StatCard } from "@/components/ui/stat-card";
import { PageHeader } from "@/components/layout/page-header";
import { ClientPicker } from "@/components/admin/client-picker";
import { analyticsApi } from "@/lib/api/analytics";
import { ActivityChart } from "@/features/analytics/activity-chart";
import { DeviceChart } from "@/features/analytics/device-chart";

const RANGES = [
  { label: "7 days", value: 7 },
  { label: "30 days", value: 30 },
];

export function AdminAnalyticsPageContent() {
  const [clientUuid, setClientUuid] = useState<string>("");
  const [days, setDays] = useState(7);

  const { data, isLoading } = useQuery({
    queryKey: ["admin", "analytics", clientUuid, days],
    queryFn: () => analyticsApi.adminSummary(clientUuid, days),
    enabled: Boolean(clientUuid),
  });

  const kpis = [
    { label: "NFC Taps", icon: Nfc, value: data?.totalNfcTaps, color: "text-primary bg-primary/10" },
    { label: "QR Scans", icon: QrCode, value: data?.totalQrScans, color: "text-primary bg-primary/10" },
    { label: "Profile Views", icon: Eye, value: data?.totalProfileViews, color: "text-primary bg-primary/10" },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Analytics"
        description="Tap and scan activity for a client's NFC cards and QR codes."
        actions={
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
            <ClientPicker value={clientUuid} onChange={setClientUuid} />
            <div className="flex gap-2">
              {RANGES.map((range) => (
                <Button
                  key={range.value}
                  size="sm"
                  variant={days === range.value ? "default" : "outline"}
                  onClick={() => setDays(range.value)}
                >
                  {range.label}
                </Button>
              ))}
            </div>
          </div>
        }
      />

      {!clientUuid ? (
        <Card>
          <CardContent className="flex flex-col items-center gap-2 py-16 text-center">
            <BarChart3 className="size-8 text-muted-foreground" />
            <p className="text-sm font-medium">Select a client</p>
            <p className="max-w-xs text-sm text-muted-foreground">
              Choose a client above to view their tap and scan analytics.
            </p>
          </CardContent>
        </Card>
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-3">
            {kpis.map((kpi) => (
              <StatCard
                key={kpi.label}
                label={kpi.label}
                value={isLoading ? "—" : (kpi.value ?? 0)}
                icon={kpi.icon}
                color={kpi.color}
                hint={`Last ${days} days`}
              />
            ))}
          </div>

          <Card>
            <CardHeader>
              <CardTitle>Activity over time</CardTitle>
              <CardDescription>NFC taps and QR scans per day.</CardDescription>
            </CardHeader>
            <CardContent>
              {isLoading || !data ? (
                <Skeleton className="h-64 w-full" />
              ) : (
                <ActivityChart data={data.dailySeries} />
              )}
            </CardContent>
          </Card>

          <div className="grid gap-4 lg:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle>Device breakdown</CardTitle>
                <CardDescription>What visitors are tapping/scanning with.</CardDescription>
              </CardHeader>
              <CardContent>
                {isLoading || !data ? (
                  <Skeleton className="h-48 w-full" />
                ) : data.deviceBreakdown.length === 0 ? (
                  <p className="py-12 text-center text-sm text-muted-foreground">No activity yet in this range.</p>
                ) : (
                  <DeviceChart data={data.deviceBreakdown} />
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Top NFC cards</CardTitle>
                <CardDescription>This client&apos;s most-tapped cards in this range.</CardDescription>
              </CardHeader>
              <CardContent>
                {isLoading || !data ? (
                  <Skeleton className="h-48 w-full" />
                ) : data.topCards.length === 0 ? (
                  <p className="py-12 text-center text-sm text-muted-foreground">No taps yet in this range.</p>
                ) : (
                  <ul className="space-y-3">
                    {data.topCards.map((card, index) => (
                      <li key={card.label} className="flex items-center justify-between text-sm">
                        <span className="flex items-center gap-2">
                          {index === 0 ? (
                            <Trophy className="size-4 text-warning" />
                          ) : (
                            <span className="w-4 text-center text-muted-foreground">{index + 1}</span>
                          )}
                          {card.label}
                        </span>
                        <span className="font-medium">{card.count} taps</span>
                      </li>
                    ))}
                  </ul>
                )}
              </CardContent>
            </Card>
          </div>
        </>
      )}
    </div>
  );
}
