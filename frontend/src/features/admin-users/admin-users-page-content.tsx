"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { UserCog } from "lucide-react";

import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import { TablePagination } from "@/components/ui/table-pagination";
import { ListSkeleton } from "@/components/ui/page-skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { StatCard } from "@/components/ui/stat-card";
import { PageHeader } from "@/components/layout/page-header";
import { ListToolbar } from "@/components/layout/list-toolbar";
import { formatDate } from "@/lib/format";
import { adminUsersApi } from "@/lib/api/admin-users";
import { CreateAdminUserDialog } from "@/features/admin-users/create-admin-user-dialog";
import { AdminUserStatusBadge } from "@/features/admin-users/admin-user-status-badge";
import { useDebouncedValue } from "@/lib/hooks/use-debounced-value";

export function AdminUsersPageContent() {
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const debouncedSearch = useDebouncedValue(search, 300);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin-users", { search: debouncedSearch, page }],
    queryFn: () => adminUsersApi.list({ search: debouncedSearch || undefined, page, size: 20 }),
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Users"
        description="Admin and Super Admin staff accounts and their permissions."
        actions={<CreateAdminUserDialog />}
      />

      <div className="grid gap-4 sm:grid-cols-3">
        <StatCard
          label="Total staff users"
          value={data?.totalElements ?? "—"}
          icon={UserCog}
          color="text-primary bg-primary/10"
        />
      </div>

      <ListToolbar
        search={search}
        onSearchChange={(value) => {
          setSearch(value);
          setPage(0);
        }}
        searchPlaceholder="Search email..."
      />

      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <ListSkeleton />
          ) : isError ? (
            <div className="p-6 text-sm text-destructive">Failed to load users.</div>
          ) : !data || data.content.length === 0 ? (
            <EmptyState
              icon={UserCog}
              title="No staff users yet"
              description="Admin and Super Admin accounts you create will appear here."
              action={<CreateAdminUserDialog />}
            />
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Email</TableHead>
                    <TableHead>Role</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Last login</TableHead>
                    <TableHead>Created</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody zebra>
                  {data.content.map((user) => (
                    <TableRow key={user.uuid} className="cursor-pointer">
                      <TableCell className="font-medium">
                        <Link href={`/admin/users/${user.uuid}`} className="hover:underline">
                          {user.email}
                        </Link>
                      </TableCell>
                      <TableCell>
                        <div className="flex flex-wrap gap-1">
                          {user.roles.map((role) => (
                            <Badge key={role} variant="secondary">
                              {role}
                            </Badge>
                          ))}
                        </div>
                      </TableCell>
                      <TableCell>
                        <AdminUserStatusBadge status={user.status} />
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {user.lastLoginAt ? formatDate(user.lastLoginAt) : "Never"}
                      </TableCell>
                      <TableCell className="text-muted-foreground">{formatDate(user.createdAt)}</TableCell>
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
