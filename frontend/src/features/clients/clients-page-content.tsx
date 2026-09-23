"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Users, UserCheck } from "lucide-react";

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
import { clientsApi } from "@/lib/api/clients";
import { CreateClientDialog } from "@/features/clients/create-client-dialog";
import { ClientStatusBadge } from "@/features/clients/client-status-badge";
import { useDebouncedValue } from "@/lib/hooks/use-debounced-value";

const ALL = "ALL";

export function ClientsPageContent() {
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<string>(ALL);
  const [page, setPage] = useState(0);
  const debouncedSearch = useDebouncedValue(search, 300);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["clients", { search: debouncedSearch, status, page }],
    queryFn: () =>
      clientsApi.list({
        search: debouncedSearch || undefined,
        status: status === ALL ? undefined : status,
        page,
        size: 20,
      }),
  });

  const { data: activeCount } = useQuery({
    queryKey: ["clients", { status: "ACTIVE", size: 1, forCount: true }],
    queryFn: () => clientsApi.list({ status: "ACTIVE", size: 1 }),
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Clients"
        description="Manage client accounts and access."
        actions={<CreateClientDialog />}
      />

      <div className="grid gap-4 sm:grid-cols-2">
        <StatCard
          label="Total clients"
          value={data?.totalElements ?? "—"}
          icon={Users}
          color="text-primary bg-primary/10"
        />
        <StatCard
          label="Active clients"
          value={activeCount?.totalElements ?? "—"}
          icon={UserCheck}
          color="text-primary bg-primary/10"
        />
      </div>

      <ListToolbar
        search={search}
        onSearchChange={(value) => {
          setSearch(value);
          setPage(0);
        }}
        searchPlaceholder="Search name or email..."
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
              <SelectItem value="ACTIVE">Active</SelectItem>
              <SelectItem value="PENDING">Pending</SelectItem>
              <SelectItem value="SUSPENDED">Suspended</SelectItem>
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
            <div className="p-6 text-sm text-destructive">Failed to load clients.</div>
          ) : !data || data.content.length === 0 ? (
            <EmptyState
              icon={Users}
              title="No clients yet"
              description="Clients you create will appear here."
              action={<CreateClientDialog />}
            />
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Client</TableHead>
                    <TableHead>Type</TableHead>
                    <TableHead>Email</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Created</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody zebra>
                  {data.content.map((client) => (
                    <TableRow key={client.uuid} className="cursor-pointer">
                      <TableCell className="font-medium">
                        <Link href={`/admin/clients/${client.uuid}`} className="hover:underline">
                          {client.displayName}
                        </Link>
                      </TableCell>
                      <TableCell className="text-muted-foreground">{client.type}</TableCell>
                      <TableCell className="text-muted-foreground">{client.email}</TableCell>
                      <TableCell>
                        <ClientStatusBadge status={client.status} />
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {formatDate(client.createdAt)}
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
