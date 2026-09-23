"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { QrCode as QrCodeIcon, Ban, CheckCircle2 } from "lucide-react";
import { toast } from "sonner";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ListSkeleton } from "@/components/ui/page-skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { PageHeader } from "@/components/layout/page-header";
import { ListToolbar } from "@/components/layout/list-toolbar";
import { ClientPicker } from "@/components/admin/client-picker";
import { adminQrCodesApi } from "@/lib/api/qr";
import { ApiClientError } from "@/lib/api/client";
import { AdminCreateQrDialog } from "@/features/qr/admin-create-qr-dialog";

export function AdminQrCodesPageContent() {
  const [clientUuid, setClientUuid] = useState<string>("");
  const queryClient = useQueryClient();

  const { data: codes, isLoading } = useQuery({
    queryKey: ["admin", "qr-codes", clientUuid],
    queryFn: () => adminQrCodesApi.listForClient(clientUuid),
    enabled: Boolean(clientUuid),
  });

  const statusMutation = useMutation({
    mutationFn: ({ uuid, active }: { uuid: string; active: boolean }) => adminQrCodesApi.setStatus(uuid, active),
    onSuccess: (updated) => {
      toast.success(updated.status === "ACTIVE" ? "QR code activated" : "QR code suspended");
      queryClient.invalidateQueries({ queryKey: ["admin", "qr-codes", clientUuid] });
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to update status");
    },
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="QR Codes"
        description="Create and manage QR codes on behalf of a client."
        actions={<AdminCreateQrDialog clientUuid={clientUuid} />}
      />

      <ListToolbar filters={<ClientPicker value={clientUuid} onChange={setClientUuid} />} />

      {!clientUuid ? (
        <Card>
          <CardContent className="p-0">
            <EmptyState
              icon={QrCodeIcon}
              title="Select a client"
              description="Choose a client above to view and manage their QR codes."
            />
          </CardContent>
        </Card>
      ) : isLoading ? (
        <Card>
          <CardContent className="p-0">
            <ListSkeleton />
          </CardContent>
        </Card>
      ) : !codes || codes.length === 0 ? (
        <Card>
          <CardContent className="p-0">
            <EmptyState
              icon={QrCodeIcon}
              title="No QR codes yet"
              description="Create one to get a scannable code for a destination."
              action={<AdminCreateQrDialog clientUuid={clientUuid} />}
            />
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {codes.map((qr) => (
            <Card key={qr.uuid}>
              <CardHeader className="flex flex-row items-start justify-between space-y-0">
                <CardTitle className="text-base">{qr.name}</CardTitle>
                <Badge variant={qr.status === "ACTIVE" ? "success" : "destructive"}>{qr.status}</Badge>
              </CardHeader>
              <CardContent className="flex flex-col items-center gap-3">
                <span className="flex size-24 items-center justify-center rounded-md border border-dashed border-border text-muted-foreground">
                  <QrCodeIcon className="size-8" />
                </span>
                <p className="text-xs text-muted-foreground">Destination: {qr.destinationName ?? "—"}</p>
                <p className="text-xs text-muted-foreground">{qr.totalScans} scans</p>
                <Button
                  variant="outline"
                  size="sm"
                  className="w-full"
                  onClick={() => statusMutation.mutate({ uuid: qr.uuid, active: qr.status !== "ACTIVE" })}
                  disabled={statusMutation.isPending}
                >
                  {qr.status === "ACTIVE" ? (
                    <>
                      <Ban className="size-4" />
                      Suspend
                    </>
                  ) : (
                    <>
                      <CheckCircle2 className="size-4" />
                      Activate
                    </>
                  )}
                </Button>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
