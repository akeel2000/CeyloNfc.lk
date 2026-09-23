"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Copy, Download, Loader2, CheckCircle2, Nfc } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { PageHeader } from "@/components/layout/page-header";
import { nfcCardRegisterSchema, type NfcCardRegisterFormValues } from "@/lib/schemas/nfc";
import { nfcCardsApi } from "@/lib/api/nfc";
import { ApiClientError } from "@/lib/api/client";
import { QrImage } from "@/features/qr/qr-image";
import type { NfcCardRegisterResult } from "@/lib/types/nfc";

export function WriteNfcPageContent() {
  const [result, setResult] = useState<NfcCardRegisterResult | null>(null);
  const queryClient = useQueryClient();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<NfcCardRegisterFormValues>({
    resolver: zodResolver(nfcCardRegisterSchema),
  });

  const registerMutation = useMutation({
    mutationFn: nfcCardsApi.register,
    onSuccess: (data) => {
      setResult(data);
      queryClient.invalidateQueries({ queryKey: ["nfc-cards"] });
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to register card");
    },
  });

  const copyUrl = async () => {
    if (!result) return;
    await navigator.clipboard.writeText(result.publicUrl);
    toast.success("Secure URL copied");
  };

  const downloadQr = () => {
    const canvas = document.querySelector<HTMLCanvasElement>("#write-nfc-qr canvas");
    if (!canvas || !result) return;
    const link = document.createElement("a");
    link.download = `${result.card.serialNumber.replace(/\s+/g, "-").toLowerCase()}.png`;
    link.href = canvas.toDataURL("image/png");
    link.click();
  };

  const registerAnother = () => {
    setResult(null);
    reset();
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Write NFC"
        description="Register a physical card and get the secure URL + QR code to write to its chip."
      />

      <Card className="mx-auto max-w-md">
        {result ? (
          <>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <CheckCircle2 className="size-5 text-success" />
                Card registered
              </CardTitle>
              <CardDescription>
                This secure URL is shown only once - write it to the physical card now (via an
                NFC-writer app scanning the QR below, or by writing the URL directly).
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex flex-col items-center gap-4 py-2" id="write-nfc-qr">
                <QrImage value={result.publicUrl} size={220} />
                <div className="flex w-full items-center gap-2 rounded-md border border-border bg-secondary/50 px-3 py-2">
                  <code className="flex-1 truncate text-sm">{result.publicUrl}</code>
                  <Button type="button" size="icon" variant="ghost" onClick={copyUrl}>
                    <Copy className="size-4" />
                  </Button>
                </div>
                <p className="text-sm text-muted-foreground">
                  Serial: <span className="font-medium text-foreground">{result.card.serialNumber}</span>
                </p>
              </div>
              <div className="flex gap-2">
                <Button type="button" variant="outline" className="flex-1" onClick={downloadQr}>
                  <Download className="size-4" />
                  Download PNG
                </Button>
                <Button type="button" className="flex-1" onClick={registerAnother}>
                  Register another
                </Button>
              </div>
            </CardContent>
          </>
        ) : (
          <>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Nfc className="size-5 text-primary" />
                Register a card
              </CardTitle>
              <CardDescription>
                Generates a cryptographically secure token for a new physical card.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleSubmit((values) => registerMutation.mutate(values))} noValidate>
                <div className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="serialNumber">Serial number</Label>
                    <Input
                      id="serialNumber"
                      placeholder="e.g. CEY-0001"
                      aria-invalid={Boolean(errors.serialNumber)}
                      {...register("serialNumber")}
                    />
                    {errors.serialNumber && (
                      <p className="text-sm text-destructive">{errors.serialNumber.message}</p>
                    )}
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="notes">Notes (optional)</Label>
                    <Input id="notes" {...register("notes")} />
                  </div>
                </div>
                <Button type="submit" className="mt-6 w-full" disabled={isSubmitting || registerMutation.isPending}>
                  {(isSubmitting || registerMutation.isPending) && <Loader2 className="size-4 animate-spin" />}
                  Register &amp; generate QR
                </Button>
              </form>
            </CardContent>
          </>
        )}
      </Card>
    </div>
  );
}
