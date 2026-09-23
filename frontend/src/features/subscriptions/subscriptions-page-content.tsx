"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { CreditCard } from "lucide-react";

import { Card, CardContent } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import { TablePagination } from "@/components/ui/table-pagination";
import { ListSkeleton } from "@/components/ui/page-skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { PageHeader } from "@/components/layout/page-header";
import { ListToolbar } from "@/components/layout/list-toolbar";
import { formatDate } from "@/lib/format";
import { subscriptionsApi } from "@/lib/api/subscriptions";
import { packagesApi } from "@/lib/api/packages";
import { SubscriptionStatusBadge } from "@/features/subscriptions/subscription-status-badge";
import type { SubscriptionStatus } from "@/lib/types/commerce";

const ALL = "ALL";
const STATUSES: SubscriptionStatus[] = ["TRIAL", "ACTIVE", "EXPIRED", "SUSPENDED", "CANCELLED"];

function formatLimit(value: number | null): string {
  return value === null ? "Unlimited" : String(value);
}

export function SubscriptionsPageContent() {
  const [status, setStatus] = useState<string>(ALL);
  const [packagePlanUuid, setPackagePlanUuid] = useState<string>(ALL);
  const [page, setPage] = useState(0);

  const { data: plans } = useQuery({
    queryKey: ["packages"],
    queryFn: () => packagesApi.listAdmin(),
  });

  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin", "subscriptions", { status, packagePlanUuid, page }],
    queryFn: () =>
      subscriptionsApi.list({
        status: status === ALL ? undefined : status,
        packagePlanUuid: packagePlanUuid === ALL ? undefined : packagePlanUuid,
        page,
        size: 20,
      }),
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Subscriptions"
        description="Every client's plan and usage, across the whole platform."
      />

      <ListToolbar
        filters={
          <div className="flex flex-wrap gap-3">
            <Select
              value={status}
              onValueChange={(value) => {
                setStatus(value);
                setPage(0);
              }}
            >
              <SelectTrigger className="sm:w-48">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>All statuses</SelectItem>
                {STATUSES.map((s) => (
                  <SelectItem key={s} value={s}>
                    {s}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Select
              value={packagePlanUuid}
              onValueChange={(value) => {
                setPackagePlanUuid(value);
                setPage(0);
              }}
            >
              <SelectTrigger className="sm:w-56">
                <SelectValue placeholder="All plans" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>All plans</SelectItem>
                {plans?.map((plan) => (
                  <SelectItem key={plan.uuid} value={plan.uuid}>
                    {plan.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        }
      />

      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <ListSkeleton />
          ) : isError ? (
            <div className="p-6 text-sm text-destructive">Failed to load subscriptions.</div>
          ) : !data || data.content.length === 0 ? (
            <EmptyState
              icon={CreditCard}
              title="No subscriptions yet"
              description="Assign a plan to a client from their detail page to see it here."
            />
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Client</TableHead>
                    <TableHead>Plan</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Cards used</TableHead>
                    <TableHead>Renews</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody zebra>
                  {data.content.map((sub) => (
                    <TableRow key={sub.uuid}>
                      <TableCell className="font-medium">
                        {sub.clientUuid ? (
                          <Link href={`/admin/clients/${sub.clientUuid}`} className="hover:underline">
                            {sub.clientDisplayName ?? "Unknown client"}
                          </Link>
                        ) : (
                          (sub.clientDisplayName ?? "Unknown client")
                        )}
                      </TableCell>
                      <TableCell className="text-muted-foreground">{sub.plan.name}</TableCell>
                      <TableCell>
                        <SubscriptionStatusBadge status={sub.status} />
                      </TableCell>
                      <TableCell>
                        {sub.cardsUsed} / {formatLimit(sub.plan.cardLimit)}
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {sub.renewalDate ? formatDate(sub.renewalDate) : "-"}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <TablePagination page={data.number} totalPages={data.totalPages} onPageChange={setPage} />
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
