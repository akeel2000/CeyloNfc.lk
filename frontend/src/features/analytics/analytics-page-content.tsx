"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Nfc, QrCode, Eye, Trophy } from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { analyticsApi } from "@/lib/api/analytics";
import { ActivityChart } from "@/features/analytics/activity-chart";
import { DeviceChart } from "@/features/analytics/device-chart";

const RANGES = [
  { label: "7 days", value: 7 },
  { label: "30 days", value: 30 },
];

export function AnalyticsPageContent() {
  const [days, setDays] = useState(7);

  const { data, isLoading } = useQuery({
    queryKey: ["client", "analytics", days],
    queryFn: () => analyticsApi.clientSummary(days),
  });

  const kpis = [
    { label: "NFC Taps", icon: Nfc, value: data?.totalNfcTaps, color: "text-primary bg-primary/10" },
    { label: "QR Scans", icon: QrCode, value: data?.totalQrScans, color: "text-primary bg-primary/10" },
    { label: "Profile Views", icon: Eye, value: data?.totalProfileViews, color: "text-primary bg-primary/10" },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Analytics</h1>
          <p className="text-sm text-muted-foreground">Tap and scan activity across your NFC cards and QR codes.</p>
        </div>
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

      <div className="grid gap-4 sm:grid-cols-3">
        {kpis.map((kpi) => (
          <Card key={kpi.label}>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">{kpi.label}</CardTitle>
              <span className={`flex size-8 items-center justify-center rounded-md ${kpi.color}`}>
                <kpi.icon className="size-4" />
              </span>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-semibold">{isLoading ? "—" : (kpi.value ?? 0)}</div>
              <p className="text-xs text-muted-foreground">Last {days} days</p>
            </CardContent>
          </Card>
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
            <CardDescription>Your most-tapped cards in this range.</CardDescription>
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
    </div>
  );
}
