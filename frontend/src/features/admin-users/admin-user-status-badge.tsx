import { Badge } from "@/components/ui/badge";
import type { AdminUserStatus } from "@/lib/types/admin-user";

const STATUS_VARIANT: Record<AdminUserStatus, "success" | "warning" | "destructive"> = {
  ACTIVE: "success",
  LOCKED: "warning",
  DISABLED: "destructive",
};

export function AdminUserStatusBadge({ status }: { status: AdminUserStatus }) {
  return <Badge variant={STATUS_VARIANT[status]}>{status}</Badge>;
}
