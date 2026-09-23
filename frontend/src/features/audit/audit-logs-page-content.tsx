"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { ScrollText } from "lucide-react";

import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import { TablePagination } from "@/components/ui/table-pagination";
import { ListSkeleton } from "@/components/ui/page-skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { PageHeader } from "@/components/layout/page-header";
import { ListToolbar } from "@/components/layout/list-toolbar";
import { formatDateTime } from "@/lib/format";
import { auditLogsApi } from "@/lib/api/audit";
import { useDebouncedValue } from "@/lib/hooks/use-debounced-value";

const ALL = "ALL";

function toIsoOrUndefined(localDateTime: string): string | undefined {
  if (!localDateTime) return undefined;
  const date = new Date(localDateTime);
  return Number.isNaN(date.getTime()) ? undefined : date.toISOString();
}

export function AuditLogsPageContent() {
  const [search, setSearch] = useState("");
  const [action, setAction] = useState<string>(ALL);
  const [entityType, setEntityType] = useState<string>(ALL);
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [page, setPage] = useState(0);
  const debouncedSearch = useDebouncedValue(search, 300);

  const { data: filters } = useQuery({
    queryKey: ["audit-logs", "filters"],
    queryFn: () => auditLogsApi.filters(),
  });

  const { data, isLoading, isError } = useQuery({
    queryKey: ["audit-logs", { search: debouncedSearch, action, entityType, from, to, page }],
    queryFn: () =>
      auditLogsApi.list({
        search: debouncedSearch || undefined,
        action: action === ALL ? undefined : action,
        entityType: entityType === ALL ? undefined : entityType,
        from: toIsoOrUndefined(from),
        to: toIsoOrUndefined(to),
        page,
        size: 25,
      }),
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Audit Logs"
        description="Append-only record of state-changing actions across the platform."
      />

      <ListToolbar
        search={search}
        onSearchChange={(value) => {
          setSearch(value);
          setPage(0);
        }}
        searchPlaceholder="Search entity/action..."
        filters={
          <div className="grid flex-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <Select
              value={action}
              onValueChange={(value) => {
                setAction(value);
                setPage(0);
              }}
            >
              <SelectTrigger>
                <SelectValue placeholder="Action" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>All actions</SelectItem>
                {filters?.actions.map((a) => (
                  <SelectItem key={a} value={a}>
                    {a}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Select
              value={entityType}
              onValueChange={(value) => {
                setEntityType(value);
                setPage(0);
              }}
            >
              <SelectTrigger>
                <SelectValue placeholder="Entity type" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>All entity types</SelectItem>
                {filters?.entityTypes.map((t) => (
                  <SelectItem key={t} value={t}>
                    {t}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <div className="space-y-1">
              <Label htmlFor="from" className="text-xs text-muted-foreground">
                From
              </Label>
              <Input
                id="from"
                type="datetime-local"
                value={from}
                onChange={(e) => {
                  setFrom(e.target.value);
                  setPage(0);
                }}
              />
            </div>
            <div className="space-y-1">
              <Label htmlFor="to" className="text-xs text-muted-foreground">
                To
              </Label>
              <Input
                id="to"
                type="datetime-local"
                value={to}
                onChange={(e) => {
                  setTo(e.target.value);
                  setPage(0);
                }}
              />
            </div>
          </div>
        }
      />

      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <ListSkeleton rows={6} />
          ) : isError ? (
            <div className="p-6 text-sm text-destructive">Failed to load audit logs.</div>
          ) : !data || data.content.length === 0 ? (
            <EmptyState icon={ScrollText} title="No matching audit log entries" />
          ) : (
            <>
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>When</TableHead>
                      <TableHead>Actor</TableHead>
                      <TableHead>Action</TableHead>
                      <TableHead>Entity</TableHead>
                      <TableHead>IP</TableHead>
                      <TableHead>Metadata</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody zebra>
                    {data.content.map((entry) => (
                      <TableRow key={entry.id}>
                        <TableCell className="whitespace-nowrap text-muted-foreground">
                          {formatDateTime(entry.createdAt)}
                        </TableCell>
                        <TableCell className="whitespace-nowrap">{entry.actorEmail}</TableCell>
                        <TableCell className="whitespace-nowrap font-mono text-xs">{entry.action}</TableCell>
                        <TableCell className="whitespace-nowrap text-muted-foreground">
                          {entry.entityType}
                          {entry.entityUuid && (
                            <span className="ml-1 font-mono text-xs">
                              {entry.entityUuid.slice(0, 8)}
                            </span>
                          )}
                        </TableCell>
                        <TableCell className="whitespace-nowrap text-muted-foreground">
                          {entry.ipAddress ?? "-"}
                        </TableCell>
                        <TableCell className="max-w-xs truncate font-mono text-xs text-muted-foreground" title={entry.metadataJson ?? undefined}>
                          {entry.metadataJson ?? "-"}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
              <TablePagination page={data.number} totalPages={data.totalPages} onPageChange={setPage} />
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
