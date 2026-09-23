import { Badge } from "@/components/ui/badge";
import type { ClientStatus } from "@/lib/types/client";

const STATUS_VARIANT: Record<ClientStatus, "success" | "warning" | "destructive" | "secondary"> = {
  ACTIVE: "success",
  PENDING: "warning",
  SUSPENDED: "destructive",
  CLOSED: "secondary",
};

export function ClientStatusBadge({ status }: { status: ClientStatus }) {
  return <Badge variant={STATUS_VARIANT[status]}>{status}</Badge>;
}
