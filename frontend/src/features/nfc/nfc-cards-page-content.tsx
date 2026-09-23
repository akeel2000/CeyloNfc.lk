"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { CheckCircle2, Nfc } from "lucide-react";

import { Card, CardContent } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import { TablePagination } from "@/components/ui/table-pagination";
import { ListSkeleton } from "@/components/ui/page-skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { StatCard } from "@/components/ui/stat-card";
import { PageHeader } from "@/components/layout/page-header";
import { ListToolbar } from "@/components/layout/list-toolbar";
import { nfcCardsApi } from "@/lib/api/nfc";
import { RegisterNfcCardDialog } from "@/features/nfc/register-nfc-card-dialog";
import { NfcStatusBadge } from "@/features/nfc/nfc-status-badge";
import { useDebouncedValue } from "@/lib/hooks/use-debounced-value";

const ALL = "ALL";

export function NfcCardsPageContent() {
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<string>(ALL);
  const [page, setPage] = useState(0);
  const debouncedSearch = useDebouncedValue(search, 300);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["nfc-cards", { search: debouncedSearch, status, page }],
    queryFn: () =>
      nfcCardsApi.list({
        search: debouncedSearch || undefined,
        status: status === ALL ? undefined : status,
        page,
        size: 20,
      }),
  });

  const { data: activeCount } = useQuery({
    queryKey: ["nfc-cards", { status: "ACTIVE", size: 1, forCount: true }],
    queryFn: () => nfcCardsApi.list({ status: "ACTIVE", size: 1 }),
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="NFC Cards"
        description="Register, assign and manage physical NFC cards."
        actions={<RegisterNfcCardDialog />}
      />

      <div className="grid gap-4 sm:grid-cols-2">
        <StatCard
          label="Total cards"
          value={data?.totalElements ?? "—"}
          icon={Nfc}
          color="text-primary bg-primary/10"
        />
        <StatCard
          label="Active cards"
          value={activeCount?.totalElements ?? "—"}
          icon={CheckCircle2}
          color="text-primary bg-primary/10"
        />
      </div>

      <ListToolbar
        search={search}
        onSearchChange={(value) => {
          setSearch(value);
          setPage(0);
        }}
        searchPlaceholder="Search serial number..."
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
              <SelectItem value="UNASSIGNED">Unassigned</SelectItem>
              <SelectItem value="ACTIVE">Active</SelectItem>
              <SelectItem value="SUSPENDED">Suspended</SelectItem>
            </SelectContent>
          </Select>
        }
      />

      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <ListSkeleton />
          ) : isError ? (
            <div className="p-6 text-sm text-destructive">Failed to load NFC cards.</div>
          ) : !data || data.content.length === 0 ? (
            <EmptyState
              icon={Nfc}
              title="No NFC cards yet"
              description="Register your first card to get a secure tap URL."
              action={<RegisterNfcCardDialog />}
            />
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Serial</TableHead>
                    <TableHead>Client</TableHead>
                    <TableHead>Destination</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Taps</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody zebra>
                  {data.content.map((card) => (
                    <TableRow key={card.uuid}>
                      <TableCell className="font-medium">
                        <Link href={`/admin/nfc-cards/${card.uuid}`} className="hover:underline">
                          {card.serialNumber}
                        </Link>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {card.clientDisplayName ?? "—"}
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {card.destinationName ?? "—"}
                      </TableCell>
                      <TableCell>
                        <NfcStatusBadge status={card.status} />
                      </TableCell>
                      <TableCell className="text-muted-foreground">{card.totalTaps}</TableCell>
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
