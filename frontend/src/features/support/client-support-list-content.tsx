"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { LifeBuoy } from "lucide-react";

import { Card, CardContent } from "@/components/ui/card";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import { ListSkeleton } from "@/components/ui/page-skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { PageHeader } from "@/components/layout/page-header";
import { formatDate } from "@/lib/format";
import { supportApi } from "@/lib/api/support";
import { TicketStatusBadge, TicketPriorityBadge } from "@/features/support/ticket-status-badge";
import { CreateTicketDialog } from "@/features/support/create-ticket-dialog";

export function ClientSupportListContent() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["support", "own"],
    queryFn: () => supportApi.listOwn(),
  });

  return (
    <div className="space-y-6">
      <PageHeader title="Support" description="Get help from our team." actions={<CreateTicketDialog />} />

      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <ListSkeleton rows={3} />
          ) : isError ? (
            <div className="p-6 text-sm text-destructive">Failed to load tickets.</div>
          ) : !data || data.length === 0 ? (
            <EmptyState
              icon={LifeBuoy}
              title="No support tickets yet"
              description="Open a ticket if you run into any issues with your cards or account."
            />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Subject</TableHead>
                  <TableHead>Priority</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Updated</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody zebra>
                {data.map((ticket) => (
                  <TableRow key={ticket.uuid} className="cursor-pointer">
                    <TableCell className="font-medium">
                      <Link href={`/client/support/${ticket.uuid}`} className="hover:underline">
                        {ticket.subject}
                      </Link>
                    </TableCell>
                    <TableCell>
                      <TicketPriorityBadge priority={ticket.priority} />
                    </TableCell>
                    <TableCell>
                      <TicketStatusBadge status={ticket.status} />
                    </TableCell>
                    <TableCell className="text-muted-foreground">{formatDate(ticket.updatedAt)}</TableCell>
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
