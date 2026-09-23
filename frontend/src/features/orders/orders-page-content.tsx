"use client";

import { useState } from "react";
import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ChevronDown, PackageCheck, ShoppingCart } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import { TablePagination } from "@/components/ui/table-pagination";
import { ListSkeleton } from "@/components/ui/page-skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { StatCard } from "@/components/ui/stat-card";
import { PageHeader } from "@/components/layout/page-header";
import { ListToolbar } from "@/components/layout/list-toolbar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { formatCurrency, formatDate } from "@/lib/format";
import { ordersApi } from "@/lib/api/orders";
import { ApiClientError } from "@/lib/api/client";
import { OrderStatusBadge, PaymentStatusBadge } from "@/features/orders/order-status-badge";
import { CreateOrderDialog } from "@/features/orders/create-order-dialog";
import { useDebouncedValue } from "@/lib/hooks/use-debounced-value";
import type { OrderStatus, PaymentStatus } from "@/lib/types/commerce";

const ALL = "ALL";
const ORDER_STATUSES: OrderStatus[] = [
  "NEW",
  "CONFIRMED",
  "DESIGNING",
  "PRINTING",
  "PROGRAMMING_NFC",
  "READY",
  "SHIPPED",
  "DELIVERED",
  "CANCELLED",
];
const PAYMENT_STATUSES: PaymentStatus[] = ["UNPAID", "PARTIALLY_PAID", "PAID", "REFUNDED"];

export function OrdersPageContent() {
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<string>(ALL);
  const [page, setPage] = useState(0);
  const debouncedSearch = useDebouncedValue(search, 300);
  const queryClient = useQueryClient();

  const { data, isLoading, isError } = useQuery({
    queryKey: ["orders", { search: debouncedSearch, status, page }],
    queryFn: () =>
      ordersApi.list({
        search: debouncedSearch || undefined,
        status: status === ALL ? undefined : status,
        page,
        size: 20,
      }),
  });

  const { data: newCount } = useQuery({
    queryKey: ["orders", { status: "NEW", size: 1, forCount: true }],
    queryFn: () => ordersApi.list({ status: "NEW", size: 1 }),
  });

  const statusMutation = useMutation({
    mutationFn: ({ uuid, next }: { uuid: string; next: OrderStatus }) => ordersApi.updateStatus(uuid, next),
    onSuccess: () => {
      toast.success("Order status updated");
      queryClient.invalidateQueries({ queryKey: ["orders"] });
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to update status");
    },
  });

  const paymentMutation = useMutation({
    mutationFn: ({ uuid, next }: { uuid: string; next: PaymentStatus }) =>
      ordersApi.updatePaymentStatus(uuid, next),
    onSuccess: () => {
      toast.success("Payment status updated");
      queryClient.invalidateQueries({ queryKey: ["orders"] });
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to update payment status");
    },
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Orders"
        description="Physical card and merchandise orders for clients."
        actions={<CreateOrderDialog />}
      />

      <div className="grid gap-4 sm:grid-cols-2">
        <StatCard
          label="Total orders"
          value={data?.totalElements ?? "—"}
          icon={ShoppingCart}
          color="text-primary bg-primary/10"
        />
        <StatCard
          label="New orders"
          value={newCount?.totalElements ?? "—"}
          icon={PackageCheck}
          color="text-primary bg-primary/10"
        />
      </div>

      <ListToolbar
        search={search}
        onSearchChange={(value) => {
          setSearch(value);
          setPage(0);
        }}
        searchPlaceholder="Search orders..."
        filters={
          <Select
            value={status}
            onValueChange={(value) => {
              setStatus(value);
              setPage(0);
            }}
          >
            <SelectTrigger className="sm:w-56">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>All statuses</SelectItem>
              {ORDER_STATUSES.map((s) => (
                <SelectItem key={s} value={s}>
                  {s.replace(/_/g, " ")}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        }
      />

      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <ListSkeleton />
          ) : isError ? (
            <div className="p-6 text-sm text-destructive">Failed to load orders.</div>
          ) : !data || data.content.length === 0 ? (
            <EmptyState
              icon={ShoppingCart}
              title="No orders yet"
              description="Orders you create will appear here."
              action={<CreateOrderDialog />}
            />
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Order</TableHead>
                    <TableHead>Client</TableHead>
                    <TableHead>Total</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Payment</TableHead>
                    <TableHead>Created</TableHead>
                    <TableHead className="w-10" />
                  </TableRow>
                </TableHeader>
                <TableBody zebra>
                  {data.content.map((order) => (
                    <TableRow key={order.uuid}>
                      <TableCell className="font-medium">
                        <Link href={`/admin/orders/${order.uuid}`} className="hover:underline">
                          {order.orderNumber}
                        </Link>
                      </TableCell>
                      <TableCell className="text-muted-foreground">{order.clientDisplayName ?? "-"}</TableCell>
                      <TableCell>{formatCurrency(order.total)}</TableCell>
                      <TableCell>
                        <OrderStatusBadge status={order.status} />
                      </TableCell>
                      <TableCell>
                        <PaymentStatusBadge status={order.paymentStatus} />
                      </TableCell>
                      <TableCell className="text-muted-foreground">{formatDate(order.createdAt)}</TableCell>
                      <TableCell>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="icon">
                              <ChevronDown className="size-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuLabel>Order status</DropdownMenuLabel>
                            {ORDER_STATUSES.map((s) => (
                              <DropdownMenuItem
                                key={s}
                                disabled={s === order.status || statusMutation.isPending}
                                onClick={() => statusMutation.mutate({ uuid: order.uuid, next: s })}
                              >
                                {s.replace(/_/g, " ")}
                              </DropdownMenuItem>
                            ))}
                            <DropdownMenuSeparator />
                            <DropdownMenuLabel>Payment status</DropdownMenuLabel>
                            {PAYMENT_STATUSES.map((s) => (
                              <DropdownMenuItem
                                key={s}
                                disabled={s === order.paymentStatus || paymentMutation.isPending}
                                onClick={() => paymentMutation.mutate({ uuid: order.uuid, next: s })}
                              >
                                {s.replace(/_/g, " ")}
                              </DropdownMenuItem>
                            ))}
                          </DropdownMenuContent>
                        </DropdownMenu>
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
