"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Copy, Loader2, RotateCcw, CheckCircle2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { nfcCardsApi } from "@/lib/api/nfc";
import { ApiClientError } from "@/lib/api/client";
import { QrImage } from "@/features/qr/qr-image";
import type { NfcCardRegisterResult } from "@/lib/types/nfc";

export function ResetNfcCardDialog({ cardUuid, onReset }: { cardUuid: string; onReset: () => void }) {
  const [open, setOpen] = useState(false);
  const [result, setResult] = useState<NfcCardRegisterResult | null>(null);
  const queryClient = useQueryClient();

  const resetMutation = useMutation({
    mutationFn: () => nfcCardsApi.reset(cardUuid),
    onSuccess: (data) => {
      setResult(data);
      queryClient.invalidateQueries({ queryKey: ["nfc-cards"] });
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to reset card");
    },
  });

  const handleOpenChange = (next: boolean) => {
    setOpen(next);
    if (!next) {
      if (result) onReset();
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
        <RotateCcw className="size-4" />
        Reset card
      </Button>
      <DialogContent>
        {result ? (
          <>
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2">
                <CheckCircle2 className="size-5 text-success" />
                Card reset
              </DialogTitle>
              <DialogDescription>
                The old link no longer works. This new secure URL is shown only once - write it
                to the card now, then assign the card to its new owner.
              </DialogDescription>
            </DialogHeader>
            <div className="flex flex-col items-center gap-4 py-2">
              <QrImage value={result.publicUrl} size={200} />
              <div className="flex w-full items-center gap-2 rounded-md border border-border bg-secondary/50 px-3 py-2">
                <code className="flex-1 truncate text-sm">{result.publicUrl}</code>
                <Button type="button" size="icon" variant="ghost" onClick={copyUrl} aria-label="Copy secure URL">
                  <Copy className="size-4" />
                </Button>
              </div>
            </div>
            <DialogFooter>
              <Button onClick={() => handleOpenChange(false)}>Done</Button>
            </DialogFooter>
          </>
        ) : (
          <>
            <DialogHeader>
              <DialogTitle>Reset this card?</DialogTitle>
              <DialogDescription>
                The link currently written on this card stops working immediately, and the card
                is removed from its client and destination. You will get a new link to write to
                the card so it can be given to someone else.
              </DialogDescription>
            </DialogHeader>
            <DialogFooter className="mt-4">
              <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>
                Cancel
              </Button>
              <Button variant="destructive" onClick={() => resetMutation.mutate()} disabled={resetMutation.isPending}>
                {resetMutation.isPending && <Loader2 className="size-4 animate-spin" />}
                Reset card
              </Button>
            </DialogFooter>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
