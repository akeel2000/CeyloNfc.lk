"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { QrCode as QrCodeIcon, Ban, CheckCircle2 } from "lucide-react";
import { toast } from "sonner";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ListSkeleton } from "@/components/ui/page-skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/layout/page-header";
import { qrCodesApi } from "@/lib/api/qr";
import { ApiClientError } from "@/lib/api/client";
import { CreateQrDialog } from "@/features/qr/create-qr-dialog";

export function QrCodesPageContent() {
  const queryClient = useQueryClient();
  const { data: codes, isLoading } = useQuery({
    queryKey: ["client", "qr-codes"],
    queryFn: () => qrCodesApi.listOwn(),
  });

  const statusMutation = useMutation({
    mutationFn: ({ uuid, active }: { uuid: string; active: boolean }) => qrCodesApi.setStatus(uuid, active),
    onSuccess: (updated) => {
      toast.success(updated.status === "ACTIVE" ? "QR code activated" : "QR code suspended");
      queryClient.invalidateQueries({ queryKey: ["client", "qr-codes"] });
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to update status");
    },
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="QR Codes"
        description="Generate and manage QR codes for your destinations. Download the image when you create a code - like the secure NFC URL, it&apos;s only shown once for security."
        actions={<CreateQrDialog />}
      />

      {isLoading ? (
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
