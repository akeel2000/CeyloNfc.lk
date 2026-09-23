"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { DetailSkeleton } from "@/components/ui/page-skeleton";
import { PageHeader } from "@/components/layout/page-header";
import { formatDate } from "@/lib/format";
import { supportApi } from "@/lib/api/support";
import { ApiClientError } from "@/lib/api/client";
import { TicketStatusBadge, TicketPriorityBadge } from "@/features/support/ticket-status-badge";
import { TicketMessages } from "@/features/support/ticket-messages";
import { ReplyForm } from "@/features/support/reply-form";
import type { MessageCreateFormValues } from "@/lib/schemas/support";

export function ClientSupportDetailContent({ uuid }: { uuid: string }) {
  const queryClient = useQueryClient();

  const { data: ticket, isLoading } = useQuery({
    queryKey: ["support", "own", uuid],
    queryFn: () => supportApi.getOwn(uuid),
  });

  const replyMutation = useMutation({
    mutationFn: (values: MessageCreateFormValues & { attachmentUrl?: string }) => supportApi.replyOwn(uuid, values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["support", "own", uuid] });
      queryClient.invalidateQueries({ queryKey: ["support", "own"] });
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to send reply");
    },
  });

  if (isLoading || !ticket) {
    return <DetailSkeleton />;
  }

  return (
    <div className="space-y-6">
      <PageHeader
        backHref="/client/support"
        title={ticket.subject}
        description={`Opened ${formatDate(ticket.createdAt)}`}
        titleExtra={
          <>
            <TicketPriorityBadge priority={ticket.priority} />
            <TicketStatusBadge status={ticket.status} />
          </>
        }
      />

      <Card>
        <CardHeader>
          <CardTitle>Conversation</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <TicketMessages messages={ticket.messages} viewerIsStaff={false} />
          <ReplyForm onSubmit={(values) => replyMutation.mutate(values)} isPending={replyMutation.isPending} />
        </CardContent>
      </Card>
    </div>
  );
}
