import { Badge } from "@/components/ui/badge";
import type { TicketPriority, TicketStatus } from "@/lib/types/support";

const STATUS_VARIANT: Record<TicketStatus, "success" | "warning" | "destructive" | "secondary" | "info"> = {
  OPEN: "info",
  IN_PROGRESS: "warning",
  RESOLVED: "success",
  CLOSED: "secondary",
};

export function TicketStatusBadge({ status }: { status: TicketStatus }) {
  return <Badge variant={STATUS_VARIANT[status]}>{status.replace("_", " ")}</Badge>;
}

const PRIORITY_VARIANT: Record<TicketPriority, "success" | "warning" | "destructive"> = {
  LOW: "success",
  MEDIUM: "warning",
  HIGH: "destructive",
};

export function TicketPriorityBadge({ priority }: { priority: TicketPriority }) {
  return <Badge variant={PRIORITY_VARIANT[priority]}>{priority}</Badge>;
}
