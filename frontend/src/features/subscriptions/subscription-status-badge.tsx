import { Badge } from "@/components/ui/badge";
import type { SubscriptionStatus } from "@/lib/types/commerce";

const STATUS_VARIANT: Record<SubscriptionStatus, "success" | "warning" | "destructive" | "secondary"> = {
  TRIAL: "warning",
  ACTIVE: "success",
  EXPIRED: "destructive",
  SUSPENDED: "destructive",
  CANCELLED: "secondary",
};

export function SubscriptionStatusBadge({ status }: { status: SubscriptionStatus }) {
  return <Badge variant={STATUS_VARIANT[status]}>{status}</Badge>;
}
