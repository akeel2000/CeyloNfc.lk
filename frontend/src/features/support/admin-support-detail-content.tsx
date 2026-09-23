"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ChevronDown } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { DetailSkeleton } from "@/components/ui/page-skeleton";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { PageHeader } from "@/components/layout/page-header";
import { supportApi } from "@/lib/api/support";
import { ApiClientError } from "@/lib/api/client";
import { TicketStatusBadge, TicketPriorityBadge } from "@/features/support/ticket-status-badge";
import { TicketMessages } from "@/features/support/ticket-messages";
import { ReplyForm } from "@/features/support/reply-form";
import type { MessageCreateFormValues } from "@/lib/schemas/support";
import type { TicketStatus } from "@/lib/types/support";

const STATUSES: TicketStatus[] = ["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"];

export function AdminSupportDetailContent({ uuid }: { uuid: string }) {
  const queryClient = useQueryClient();

  const { data: ticket, isLoading } = useQuery({
    queryKey: ["support", "admin", uuid],
    queryFn: () => supportApi.getAdmin(uuid),
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["support", "admin", uuid] });
    queryClient.invalidateQueries({ queryKey: ["support", "admin"] });
  };

  const replyMutation = useMutation({
    mutationFn: (values: MessageCreateFormValues & { attachmentUrl?: string }) => supportApi.replyAdmin(uuid, values),
    onSuccess: invalidate,
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to send reply");
    },
  });

  const statusMutation = useMutation({
    mutationFn: (status: TicketStatus) => supportApi.updateStatus(uuid, status),
    onSuccess: () => {
      toast.success("Ticket status updated");
      invalidate();
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to update status");
    },
  });

  if (isLoading || !ticket) {
    return <DetailSkeleton />;
  }

  return (
    <div className="space-y-6">
      <PageHeader
        backHref="/admin/support"
        title={ticket.subject}
        description={ticket.clientDisplayName}
        titleExtra={<TicketPriorityBadge priority={ticket.priority} />}
        actions={
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="outline" size="sm">
                <TicketStatusBadge status={ticket.status} />
                <ChevronDown className="size-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              {STATUSES.map((s) => (
                <DropdownMenuItem
                  key={s}
                  disabled={s === ticket.status || statusMutation.isPending}
                  onClick={() => statusMutation.mutate(s)}
                >
                  {s.replace("_", " ")}
                </DropdownMenuItem>
              ))}
            </DropdownMenuContent>
          </DropdownMenu>
        }
      />

      <Card>
        <CardHeader>
          <CardTitle>Conversation</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <TicketMessages messages={ticket.messages} viewerIsStaff={true} />
          <ReplyForm onSubmit={(values) => replyMutation.mutate(values)} isPending={replyMutation.isPending} />
        </CardContent>
      </Card>
    </div>
  );
}
