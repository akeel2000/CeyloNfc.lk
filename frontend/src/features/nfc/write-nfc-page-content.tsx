"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Copy, Download, Loader2, CheckCircle2, Nfc, PenLine, Smartphone, Link2 } from "lucide-react";
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
    <div className="mx-auto max-w-2xl space-y-5 sm:space-y-6">
      <PageHeader
        title="Write NFC"
        description="Create a secure card link, write it with your NFC tool, then assign and activate the card."
      />

      {!result && (
        <div className="grid gap-2 rounded-lg border border-border bg-background p-3 text-sm sm:grid-cols-3 sm:gap-0 sm:p-2">
          <WriteStep icon={Link2} number="1" title="Create link" description="Register the card" />
          <WriteStep icon={PenLine} number="2" title="Write card" description="Use your NFC writer" />
          <WriteStep icon={Smartphone} number="3" title="Tap to test" description="Then assign & activate" />
        </div>
      )}

      <Card className="mx-auto w-full max-w-xl">
        {result ? (
          <>
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2">
                <CheckCircle2 className="size-5 text-success" />
                Card registered
              </CardTitle>
              <CardDescription>
                Save this secure URL now. It is shown only once and is the only link that should
                be written to the physical card.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-5">
              <div className="rounded-lg border border-primary/30 bg-primary/10 p-3 text-sm text-foreground">
                <p className="font-medium">Next: write this URL to your card</p>
                <p className="mt-1 text-muted-foreground">In NFC Tools choose Write → Add a record → URL / URI.</p>
              </div>
              <div className="flex flex-col items-center gap-4" id="write-nfc-qr">
                <QrImage value={result.publicUrl} size={220} />
                <div className="flex w-full items-center gap-2 rounded-md border border-border bg-secondary/50 p-2">
                  <code className="min-w-0 flex-1 break-all text-xs leading-5 sm:text-sm">{result.publicUrl}</code>
                  <Button type="button" size="icon" variant="ghost" className="h-11 w-11 shrink-0" onClick={copyUrl} aria-label="Copy secure URL">
                    <Copy className="size-4" />
                  </Button>
                </div>
                <p className="w-full text-center text-sm text-muted-foreground">
                  Serial: <span className="font-medium text-foreground">{result.card.serialNumber}</span>
                </p>
              </div>
              <div className="grid gap-2 sm:grid-cols-2">
                <Button type="button" variant="outline" className="h-11 w-full" onClick={downloadQr}>
                  <Download className="size-4" />
                  Download PNG
                </Button>
                <Button type="button" className="h-11 w-full" onClick={registerAnother}>
                  Register another
                </Button>
              </div>
            </CardContent>
          </>
        ) : (
          <>
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2">
                <Nfc className="size-5 text-primary" />
                Register a card
              </CardTitle>
              <CardDescription>Use the serial number printed on the card or your inventory label.</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleSubmit((values) => registerMutation.mutate(values))} noValidate>
                <div className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="serialNumber">Serial number</Label>
                    <Input
                      id="serialNumber"
                      placeholder="e.g. CEY-0001"
                      className="h-11 text-base"
                      aria-invalid={Boolean(errors.serialNumber)}
                      {...register("serialNumber")}
                    />
                    {errors.serialNumber && (
                      <p className="text-sm text-destructive">{errors.serialNumber.message}</p>
                    )}
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="notes">Notes (optional)</Label>
                    <Input id="notes" className="h-11 text-base" placeholder="e.g. Google Review card - Colombo" {...register("notes")} />
                  </div>
                </div>
                <Button type="submit" className="mt-6 h-11 w-full text-base" disabled={isSubmitting || registerMutation.isPending}>
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

function WriteStep({
  icon: Icon,
  number,
  title,
  description,
}: {
  icon: typeof Nfc;
  number: string;
  title: string;
  description: string;
}) {
  return (
    <div className="flex items-center gap-3 rounded-md px-2 py-2 sm:border-r sm:border-border sm:last:border-r-0">
      <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-primary text-sm font-semibold text-primary-foreground">
        {number}
      </span>
      <Icon className="size-4 shrink-0 text-primary" />
      <div className="min-w-0">
        <p className="font-medium leading-tight">{title}</p>
        <p className="text-xs text-muted-foreground">{description}</p>
      </div>
    </div>
  );
}
