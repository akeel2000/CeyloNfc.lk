"use client";

import { useQuery } from "@tanstack/react-query";
import { ShoppingCart } from "lucide-react";

import { Card, CardContent } from "@/components/ui/card";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import { ListSkeleton } from "@/components/ui/page-skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { PageHeader } from "@/components/layout/page-header";
import { formatCurrency, formatDate } from "@/lib/format";
import { ordersApi } from "@/lib/api/orders";
import { OrderStatusBadge, PaymentStatusBadge } from "@/features/orders/order-status-badge";

export function ClientOrdersPageContent() {
  const { data: orders, isLoading, isError } = useQuery({
    queryKey: ["client", "orders"],
    queryFn: () => ordersApi.listOwn(),
  });

  return (
    <div className="space-y-6">
      <PageHeader title="Orders" description="Physical card and merchandise orders for your account." />

      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <ListSkeleton rows={3} />
          ) : isError ? (
            <div className="p-6 text-sm text-destructive">Failed to load orders.</div>
          ) : !orders || orders.length === 0 ? (
            <EmptyState
              icon={ShoppingCart}
              title="No orders yet"
              description="Orders placed on your behalf will appear here."
            />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Order</TableHead>
                  <TableHead>Items</TableHead>
                  <TableHead>Total</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Payment</TableHead>
                  <TableHead>Created</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody zebra>
                {orders.map((order) => (
                  <TableRow key={order.uuid}>
                    <TableCell className="font-medium">{order.orderNumber}</TableCell>
                    <TableCell className="text-muted-foreground">
                      {order.items.map((item) => `${item.quantity}x ${item.productName}`).join(", ")}
                    </TableCell>
                    <TableCell>{formatCurrency(order.total)}</TableCell>
                    <TableCell>
                      <OrderStatusBadge status={order.status} />
                    </TableCell>
                    <TableCell>
                      <PaymentStatusBadge status={order.paymentStatus} />
                    </TableCell>
                    <TableCell className="text-muted-foreground">{formatDate(order.createdAt)}</TableCell>
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
