"use client";

import { useQuery } from "@tanstack/react-query";
import { Package } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import { ListSkeleton } from "@/components/ui/page-skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { PageHeader } from "@/components/layout/page-header";
import { formatCurrency } from "@/lib/format";
import { packagesApi } from "@/lib/api/packages";
import { PackagePlanDialog } from "@/features/packages/package-plan-dialog";

function formatLimit(value: number | null): string {
  return value === null ? "Unlimited" : String(value);
}

export function PackagesPageContent() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["packages"],
    queryFn: () => packagesApi.listAdmin(),
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Packages"
        description="Manage subscription plans shown on the public pricing page."
        actions={<PackagePlanDialog />}
      />

      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <ListSkeleton />
          ) : isError ? (
            <div className="p-6 text-sm text-destructive">Failed to load packages.</div>
          ) : !data || data.length === 0 ? (
            <EmptyState
              icon={Package}
              title="No packages yet"
              description="Create a plan to make it available on the public pricing page."
              action={<PackagePlanDialog />}
            />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Price</TableHead>
                  <TableHead>Billing</TableHead>
                  <TableHead>Card limit</TableHead>
                  <TableHead>Profile limit</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="w-10" />
                </TableRow>
              </TableHeader>
              <TableBody zebra>
                {data
                  .slice()
                  .sort((a, b) => a.sortOrder - b.sortOrder)
                  .map((plan) => (
                    <TableRow key={plan.uuid}>
                      <TableCell className="font-medium">{plan.name}</TableCell>
                      <TableCell>{formatCurrency(plan.price)}</TableCell>
                      <TableCell className="text-muted-foreground">{plan.billingPeriod}</TableCell>
                      <TableCell className="text-muted-foreground">{formatLimit(plan.cardLimit)}</TableCell>
                      <TableCell className="text-muted-foreground">{formatLimit(plan.profileLimit)}</TableCell>
                      <TableCell>
                        <Badge variant={plan.active ? "success" : "secondary"}>
                          {plan.active ? "Active" : "Inactive"}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <PackagePlanDialog existing={plan} />
                      </TableCell>
                    </TableRow>
                  ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
