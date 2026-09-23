"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Copy, Loader2, Plus, CheckCircle2 } from "lucide-react";
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
import { nfcCardRegisterSchema, type NfcCardRegisterFormValues } from "@/lib/schemas/nfc";
import { nfcCardsApi } from "@/lib/api/nfc";
import { ApiClientError } from "@/lib/api/client";
import type { NfcCardRegisterResult } from "@/lib/types/nfc";

export function RegisterNfcCardDialog() {
  const [open, setOpen] = useState(false);
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
      <Button onClick={() => setOpen(true)}>
        <Plus className="size-4" />
        Register NFC Card
      </Button>
      <DialogContent>
        {result ? (
          <>
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2">
                <CheckCircle2 className="size-5 text-success" />
                Card registered
              </DialogTitle>
              <DialogDescription>
                This secure URL is shown only once - copy it now, or use the Write NFC page next
                time to get a scannable QR code for the writer app as well.
              </DialogDescription>
            </DialogHeader>
            <div className="flex items-center gap-2 rounded-md border border-border bg-secondary/50 px-3 py-2">
              <code className="flex-1 truncate text-sm">{result.publicUrl}</code>
              <Button type="button" size="icon" variant="ghost" onClick={copyUrl}>
                <Copy className="size-4" />
              </Button>
            </div>
            <DialogFooter>
              <Button onClick={() => handleOpenChange(false)}>Done</Button>
            </DialogFooter>
          </>
        ) : (
          <form onSubmit={handleSubmit((values) => registerMutation.mutate(values))} noValidate>
            <DialogHeader>
              <DialogTitle>Register NFC Card</DialogTitle>
              <DialogDescription>
                Generates a cryptographically secure token for this physical card.
              </DialogDescription>
            </DialogHeader>

            <div className="mt-4 space-y-4">
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

            <DialogFooter className="mt-6">
              <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>
                Cancel
              </Button>
              <Button type="submit" disabled={isSubmitting || registerMutation.isPending}>
                {(isSubmitting || registerMutation.isPending) && <Loader2 className="size-4 animate-spin" />}
                Register
              </Button>
            </DialogFooter>
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
}
