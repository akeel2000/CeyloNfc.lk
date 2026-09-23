"use client";

import { useQuery } from "@tanstack/react-query";
import { Printer } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import { DetailSkeleton } from "@/components/ui/page-skeleton";
import { PageHeader } from "@/components/layout/page-header";
import { formatCurrency, formatDateTime } from "@/lib/format";
import { ordersApi } from "@/lib/api/orders";
import { OrderStatusBadge, PaymentStatusBadge } from "@/features/orders/order-status-badge";
import { brand } from "@/lib/config/brand";

export function OrderDetailContent({ uuid }: { uuid: string }) {
  const { data: order, isLoading, isError } = useQuery({
    queryKey: ["orders", uuid],
    queryFn: () => ordersApi.get(uuid),
  });

  if (isLoading) {
    return <DetailSkeleton />;
  }

  if (isError || !order) {
    return <div className="text-sm text-destructive">Failed to load order.</div>;
  }

  return (
    <div className="space-y-6">
      <PageHeader
        backHref="/admin/orders"
        title={`Order ${order.orderNumber}`}
        description={`Placed ${formatDateTime(order.createdAt)}`}
        titleExtra={
          <div className="flex flex-wrap items-center gap-2">
            <OrderStatusBadge status={order.status} />
            <PaymentStatusBadge status={order.paymentStatus} />
          </div>
        }
        actions={
          <Button onClick={() => window.print()}>
            <Printer className="size-4" />
            Print
          </Button>
        }
        className="print:hidden"
      />

      <Card className="print:border-none print:shadow-none">
        <CardHeader className="flex flex-row items-start justify-between space-y-0">
          <div>
            <p className="hidden text-lg font-semibold print:block">{brand.name}</p>
            <CardTitle className="hidden text-xl print:block">Order {order.orderNumber}</CardTitle>
            <p className="hidden text-sm text-muted-foreground print:block">
              Placed {formatDateTime(order.createdAt)}
            </p>
          </div>
          <div className="hidden flex-col items-end gap-2 print:flex">
            <OrderStatusBadge status={order.status} />
            <PaymentStatusBadge status={order.paymentStatus} />
          </div>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <p className="text-xs font-medium uppercase text-muted-foreground">Client</p>
              <p className="text-sm">{order.clientDisplayName ?? "-"}</p>
            </div>
            {order.notes && (
              <div>
                <p className="text-xs font-medium uppercase text-muted-foreground">Notes</p>
                <p className="text-sm">{order.notes}</p>
              </div>
            )}
          </div>

          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Product</TableHead>
                <TableHead className="text-right">Unit price</TableHead>
                <TableHead className="text-right">Qty</TableHead>
                <TableHead className="text-right">Line total</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {order.items.map((item, index) => (
                <TableRow key={item.productUuid ?? index}>
                  <TableCell>{item.productName}</TableCell>
                  <TableCell className="text-right">{formatCurrency(item.unitPrice)}</TableCell>
                  <TableCell className="text-right">{item.quantity}</TableCell>
                  <TableCell className="text-right">{formatCurrency(item.unitPrice * item.quantity)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>

          <div className="ml-auto flex w-full max-w-xs flex-col gap-1 border-t border-border pt-3 text-sm sm:ml-auto">
            <div className="flex justify-between text-muted-foreground">
              <span>Subtotal</span>
              <span>{formatCurrency(order.subtotal)}</span>
            </div>
            <div className="flex justify-between text-base font-semibold">
              <span>Total</span>
              <span>{formatCurrency(order.total)}</span>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
