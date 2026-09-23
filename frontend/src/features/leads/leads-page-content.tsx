"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Contact, UserPlus } from "lucide-react";

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
import { leadsApi } from "@/lib/api/leads";
import { LeadStatusBadge } from "@/features/leads/lead-status-badge";
import { LeadUpdateDialog } from "@/features/leads/lead-update-dialog";
import { ConvertLeadDialog } from "@/features/leads/convert-lead-dialog";

const ALL = "ALL";

export function LeadsPageContent() {
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<string>(ALL);
  const [page, setPage] = useState(0);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["leads", { search, status, page }],
    queryFn: () =>
      leadsApi.list({
        search: search || undefined,
        status: status === ALL ? undefined : status,
        page,
        size: 20,
      }),
  });

  const { data: newCount } = useQuery({
    queryKey: ["leads", { status: "NEW", size: 1, forCount: true }],
    queryFn: () => leadsApi.list({ status: "NEW", size: 1 }),
  });

  return (
    <div className="space-y-6">
      <PageHeader title="Leads" description="Sales inquiries submitted through the public site." />

      <div className="grid gap-4 sm:grid-cols-2">
        <StatCard
          label="Total leads"
          value={data?.totalElements ?? "—"}
          icon={Contact}
          color="text-primary bg-primary/10"
        />
        <StatCard
          label="New leads"
          value={newCount?.totalElements ?? "—"}
          icon={UserPlus}
          color="text-primary bg-primary/10"
        />
      </div>

      <ListToolbar
        search={search}
        onSearchChange={(value) => {
          setSearch(value);
          setPage(0);
        }}
        searchPlaceholder="Search name, email, company..."
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
              <SelectItem value="NEW">New</SelectItem>
              <SelectItem value="CONTACTED">Contacted</SelectItem>
              <SelectItem value="CONVERTED">Converted</SelectItem>
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
            <div className="p-6 text-sm text-destructive">Failed to load leads.</div>
          ) : !data || data.content.length === 0 ? (
            <EmptyState
              icon={Contact}
              title="No leads yet"
              description="Submissions from the public contact form will appear here."
            />
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Name</TableHead>
                    <TableHead>Company</TableHead>
                    <TableHead>Email</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Received</TableHead>
                    <TableHead className="w-20" />
                  </TableRow>
                </TableHeader>
                <TableBody zebra>
                  {data.content.map((lead) => (
                    <TableRow key={lead.uuid}>
                      <TableCell className="font-medium">{lead.name}</TableCell>
                      <TableCell className="text-muted-foreground">{lead.company ?? "-"}</TableCell>
                      <TableCell className="text-muted-foreground">{lead.email}</TableCell>
                      <TableCell>
                        <LeadStatusBadge status={lead.status} />
                      </TableCell>
                      <TableCell className="text-muted-foreground">{formatDate(lead.createdAt)}</TableCell>
                      <TableCell>
                        <div className="flex items-center gap-1">
                          <ConvertLeadDialog lead={lead} />
                          <LeadUpdateDialog lead={lead} />
                        </div>
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
