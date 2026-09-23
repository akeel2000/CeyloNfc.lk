import { Badge } from "@/components/ui/badge";
import type { NfcCardStatus } from "@/lib/types/nfc";

const STATUS_VARIANT: Record<NfcCardStatus, "success" | "warning" | "destructive" | "secondary" | "info"> = {
  ACTIVE: "success",
  UNASSIGNED: "secondary",
  INACTIVE: "secondary",
  SUSPENDED: "destructive",
  LOST: "destructive",
  EXPIRED: "warning",
  REPLACED: "info",
};

export function NfcStatusBadge({ status }: { status: NfcCardStatus }) {
  return <Badge variant={STATUS_VARIANT[status]}>{status}</Badge>;
}
