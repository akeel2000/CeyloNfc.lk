"use client";

import { useQuery } from "@tanstack/react-query";
import { Nfc, Link2 } from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { ListSkeleton } from "@/components/ui/page-skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { PageHeader } from "@/components/layout/page-header";
import { formatDate } from "@/lib/format";
import { clientNfcCardsApi } from "@/lib/api/nfc";
import { NfcStatusBadge } from "@/features/nfc/nfc-status-badge";

export function ClientNfcCardsContent() {
  const { data, isLoading } = useQuery({
    queryKey: ["client", "nfc-cards"],
    queryFn: () => clientNfcCardsApi.list(),
  });

  return (
    <div className="space-y-6">
      <PageHeader title="NFC Cards" description="Cards assigned to your account." />

      {isLoading ? (
        <Card>
          <CardContent className="p-0">
            <ListSkeleton />
          </CardContent>
        </Card>
      ) : !data || data.length === 0 ? (
        <Card>
          <CardContent className="p-0">
            <EmptyState
              icon={Nfc}
              title="No NFC cards yet"
              description="Your assigned NFC cards will appear here once an admin registers and assigns one to your account."
            />
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {data.map((card) => (
            <Card key={card.uuid}>
              <CardHeader className="flex flex-row items-center justify-between space-y-0">
                <div>
                  <CardTitle className="text-base">{card.serialNumber}</CardTitle>
                  <CardDescription>
                    {card.destinationName ? (
                      <span className="flex items-center gap-1">
                        <Link2 className="size-3" />
                        {card.destinationName}
                      </span>
                    ) : (
                      "No destination set"
                    )}
                  </CardDescription>
                </div>
                <NfcStatusBadge status={card.status} />
              </CardHeader>
              <CardContent className="flex items-center justify-between text-sm text-muted-foreground">
                <span>{card.totalTaps} taps</span>
                {card.lastTappedAt && <span>Last tap {formatDate(card.lastTappedAt)}</span>}
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
