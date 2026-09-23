import { Badge } from "@/components/ui/badge";
import type { OrderStatus, PaymentStatus } from "@/lib/types/commerce";

const STATUS_VARIANT: Record<OrderStatus, "success" | "warning" | "destructive" | "secondary" | "info"> = {
  NEW: "secondary",
  CONFIRMED: "info",
  DESIGNING: "info",
  PRINTING: "info",
  PROGRAMMING_NFC: "info",
  READY: "warning",
  SHIPPED: "warning",
  DELIVERED: "success",
  CANCELLED: "destructive",
};

export function OrderStatusBadge({ status }: { status: OrderStatus }) {
  return <Badge variant={STATUS_VARIANT[status]}>{status.replace(/_/g, " ")}</Badge>;
}

const PAYMENT_VARIANT: Record<PaymentStatus, "success" | "warning" | "destructive" | "secondary"> = {
  UNPAID: "destructive",
  PARTIALLY_PAID: "warning",
  PAID: "success",
  REFUNDED: "secondary",
};

export function PaymentStatusBadge({ status }: { status: PaymentStatus }) {
  return <Badge variant={PAYMENT_VARIANT[status]}>{status.replace(/_/g, " ")}</Badge>;
}
