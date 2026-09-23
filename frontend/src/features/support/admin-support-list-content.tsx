"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { LifeBuoy, AlertCircle } from "lucide-react";

import { Card, CardContent } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import { TablePagination } from "@/components/ui/table-pagination";
import { ListSkeleton } from "@/components/ui/page-skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { StatCard } from "@/components/ui/stat-card";
import { PageHeader } from "@/components/layout/page-header";
import { ListToolbar } from "@/components/layout/list-toolbar";
import { formatDate } from "@/lib/format";
import { supportApi } from "@/lib/api/support";
import { useDebouncedValue } from "@/lib/hooks/use-debounced-value";
import { TicketStatusBadge, TicketPriorityBadge } from "@/features/support/ticket-status-badge";

const ALL = "ALL";

export function AdminSupportListContent() {
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<string>(ALL);
  const [page, setPage] = useState(0);
  const debouncedSearch = useDebouncedValue(search, 300);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["support", "admin", { search: debouncedSearch, status, page }],
    queryFn: () =>
      supportApi.listAdmin({
        search: debouncedSearch || undefined,
        status: status === ALL ? undefined : status,
        page,
        size: 20,
      }),
  });

  const { data: openCount } = useQuery({
    queryKey: ["support", "admin", { status: "OPEN", size: 1, forCount: true }],
    queryFn: () => supportApi.listAdmin({ status: "OPEN", size: 1 }),
  });

  return (
    <div className="space-y-6">
      <PageHeader title="Support" description="Client support tickets." />

      <div className="grid gap-4 sm:grid-cols-2">
        <StatCard
          label="Total tickets"
          value={data?.totalElements ?? "—"}
          icon={LifeBuoy}
          color="text-primary bg-primary/10"
        />
        <StatCard
          label="Open tickets"
          value={openCount?.totalElements ?? "—"}
          icon={AlertCircle}
          color="text-primary bg-primary/10"
        />
      </div>

      <ListToolbar
        search={search}
        onSearchChange={(value) => {
          setSearch(value);
          setPage(0);
        }}
        searchPlaceholder="Search tickets..."
        filters={
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
              <SelectItem value="OPEN">Open</SelectItem>
              <SelectItem value="IN_PROGRESS">In progress</SelectItem>
              <SelectItem value="RESOLVED">Resolved</SelectItem>
              <SelectItem value="CLOSED">Closed</SelectItem>
            </SelectContent>
          </Select>
        }
      />

      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <ListSkeleton />
          ) : isError ? (
            <div className="p-6 text-sm text-destructive">Failed to load tickets.</div>
          ) : !data || data.content.length === 0 ? (
            <EmptyState
              icon={LifeBuoy}
              title="No support tickets"
              description="Tickets clients open will appear here."
            />
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Subject</TableHead>
                    <TableHead>Client</TableHead>
                    <TableHead>Priority</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Updated</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody zebra>
                  {data.content.map((ticket) => (
                    <TableRow key={ticket.uuid} className="cursor-pointer">
                      <TableCell className="font-medium">
                        <Link href={`/admin/support/${ticket.uuid}`} className="hover:underline">
                          {ticket.subject}
                        </Link>
                      </TableCell>
                      <TableCell className="text-muted-foreground">{ticket.clientDisplayName ?? "-"}</TableCell>
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
              <TablePagination page={data.number} totalPages={data.totalPages} onPageChange={setPage} />
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
