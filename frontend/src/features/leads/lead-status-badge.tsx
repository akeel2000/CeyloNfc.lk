import { Badge } from "@/components/ui/badge";
import type { LeadStatus } from "@/lib/types/lead";

const STATUS_VARIANT: Record<LeadStatus, "success" | "warning" | "destructive" | "secondary" | "info"> = {
  NEW: "info",
  CONTACTED: "warning",
  CONVERTED: "success",
  CLOSED: "secondary",
};

export function LeadStatusBadge({ status }: { status: LeadStatus }) {
  return <Badge variant={STATUS_VARIANT[status]}>{status}</Badge>;
}
