"use client";

import { useState } from "react";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Copy, Loader2, Repeat, CheckCircle2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { nfcCardReplaceSchema, type NfcCardReplaceFormValues } from "@/lib/schemas/nfc";
import { nfcCardsApi } from "@/lib/api/nfc";
import { ApiClientError } from "@/lib/api/client";
import { QrImage } from "@/features/qr/qr-image";
import type { NfcCardRegisterResult } from "@/lib/types/nfc";

export function ReplaceNfcCardDialog({ cardUuid, onReplaced }: { cardUuid: string; onReplaced: () => void }) {
  const [open, setOpen] = useState(false);
  const [result, setResult] = useState<NfcCardRegisterResult | null>(null);
  const queryClient = useQueryClient();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<NfcCardReplaceFormValues>({
    resolver: zodResolver(nfcCardReplaceSchema),
  });

  const replaceMutation = useMutation({
    mutationFn: (values: NfcCardReplaceFormValues) => nfcCardsApi.replace(cardUuid, values),
    onSuccess: (data) => {
      setResult(data);
      queryClient.invalidateQueries({ queryKey: ["nfc-cards"] });
      onReplaced();
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to replace card");
    },
  });

  const handleOpenChange = (next: boolean) => {
    setOpen(next);
    if (!next) {
      reset();
      setResult(null);
    }
  };

  const copyUrl = async () => {
    if (!result) return;
    await navigator.clipboard.writeText(result.publicUrl);
    toast.success("Secure URL copied");
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <Button variant="outline" onClick={() => setOpen(true)}>
        <Repeat className="size-4" />
        Replace card
      </Button>
      <DialogContent>
        {result ? (
          <>
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2">
                <CheckCircle2 className="size-5 text-success" />
                Card replaced
              </DialogTitle>
              <DialogDescription>
                The old card is now inactive. This new secure URL is shown only once - write it
                to the replacement chip now.
              </DialogDescription>
            </DialogHeader>
            <div className="flex flex-col items-center gap-4 py-2">
              <QrImage value={result.publicUrl} size={200} />
              <div className="flex w-full items-center gap-2 rounded-md border border-border bg-secondary/50 px-3 py-2">
                <code className="flex-1 truncate text-sm">{result.publicUrl}</code>
                <Button type="button" size="icon" variant="ghost" onClick={copyUrl}>
                  <Copy className="size-4" />
                </Button>
              </div>
              <p className="text-sm text-muted-foreground">
                Same client and destination as before - serial{" "}
                <span className="font-medium text-foreground">{result.card.serialNumber}</span>
              </p>
            </div>
            <DialogFooter className="sm:justify-between">
              <Button asChild variant="outline">
                <Link href={`/admin/nfc-cards/${result.card.uuid}`} onClick={() => handleOpenChange(false)}>
                  View new card
                </Link>
              </Button>
              <Button onClick={() => handleOpenChange(false)}>Done</Button>
            </DialogFooter>
          </>
        ) : (
          <form onSubmit={handleSubmit((values) => replaceMutation.mutate(values))} noValidate>
            <DialogHeader>
              <DialogTitle>Replace this card</DialogTitle>
              <DialogDescription>
                Registers a new physical card with the same client and destination as this one,
                then marks this card as replaced.
              </DialogDescription>
            </DialogHeader>

            <div className="mt-4 space-y-4">
              <div className="space-y-2">
                <Label htmlFor="newSerialNumber">New serial number</Label>
                <Input
                  id="newSerialNumber"
                  placeholder="e.g. CEY-0002"
                  aria-invalid={Boolean(errors.newSerialNumber)}
                  {...register("newSerialNumber")}
                />
                {errors.newSerialNumber && (
                  <p className="text-sm text-destructive">{errors.newSerialNumber.message}</p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="notes">Notes (optional)</Label>
                <Input id="notes" placeholder="e.g. Original card lost" {...register("notes")} />
              </div>
            </div>

            <DialogFooter className="mt-6">
              <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>
                Cancel
              </Button>
              <Button type="submit" disabled={isSubmitting || replaceMutation.isPending}>
                {(isSubmitting || replaceMutation.isPending) && <Loader2 className="size-4 animate-spin" />}
                Replace
              </Button>
            </DialogFooter>
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
}
