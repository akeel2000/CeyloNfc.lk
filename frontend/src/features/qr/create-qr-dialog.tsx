"use client";

import { useState } from "react";
import { Controller, useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
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
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { qrCreateSchema, type QrCreateFormValues } from "@/lib/schemas/qr";
import { qrCodesApi } from "@/lib/api/qr";
import { reviewLocationsApi } from "@/lib/api/review";
import { ApiClientError } from "@/lib/api/client";
import { QrCustomizationPanel } from "@/features/qr/qr-customization-panel";
import type { QrCodeCreateResult } from "@/lib/types/qr";

export function CreateQrDialog() {
  const [open, setOpen] = useState(false);
  const [result, setResult] = useState<QrCodeCreateResult | null>(null);
  const queryClient = useQueryClient();

  const { data: reviewLocations } = useQuery({
    queryKey: ["client", "google-reviews"],
    queryFn: () => reviewLocationsApi.listOwn(),
  });

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<QrCreateFormValues>({
    resolver: zodResolver(qrCreateSchema),
    defaultValues: { destinationType: "WEBSITE" },
  });
  const destinationType = useWatch({ control, name: "destinationType" });

  const createMutation = useMutation({
    mutationFn: qrCodesApi.create,
    onSuccess: (data) => {
      setResult(data);
      queryClient.invalidateQueries({ queryKey: ["client", "qr-codes"] });
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to create QR code");
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
    toast.success("Link copied");
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <Button onClick={() => setOpen(true)}>
        <Plus className="size-4" />
        Create QR Code
      </Button>
      <DialogContent>
        {result ? (
          <>
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2">
                <CheckCircle2 className="size-5 text-success" />
                QR code created
              </DialogTitle>
              <DialogDescription>Customize the style, download the image, or copy the link to share it.</DialogDescription>
            </DialogHeader>
            <div className="space-y-4 py-2">
              <div className="flex w-full items-center gap-2 rounded-md border border-border bg-secondary/50 px-3 py-2">
                <code className="flex-1 truncate text-sm">{result.publicUrl}</code>
                <Button type="button" size="icon" variant="ghost" onClick={copyUrl}>
                  <Copy className="size-4" />
                </Button>
              </div>
              <QrCustomizationPanel
                publicUrl={result.publicUrl}
                fileName={result.qrCode.name.replace(/\s+/g, "-").toLowerCase()}
              />
            </div>
            <DialogFooter>
              <Button onClick={() => handleOpenChange(false)}>Done</Button>
            </DialogFooter>
          </>
        ) : (
          <form onSubmit={handleSubmit((values) => createMutation.mutate(values))} noValidate>
            <DialogHeader>
              <DialogTitle>Create QR Code</DialogTitle>
              <DialogDescription>Generates a scannable QR code linked to a destination.</DialogDescription>
            </DialogHeader>

            <div className="mt-4 space-y-4">
              <div className="space-y-2">
                <Label htmlFor="name">Name</Label>
                <Input id="name" placeholder="e.g. Table QR" aria-invalid={Boolean(errors.name)} {...register("name")} />
                {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
              </div>

              <div className="space-y-2">
                <Label>Destination type</Label>
                <Controller
                  name="destinationType"
                  control={control}
                  render={({ field }) => (
                    <Select value={field.value} onValueChange={field.onChange}>
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="GOOGLE_REVIEW">Google Review</SelectItem>
                        <SelectItem value="VCARD">Download vCard</SelectItem>
                        <SelectItem value="WEBSITE">Website</SelectItem>
                        <SelectItem value="WHATSAPP">WhatsApp</SelectItem>
                        <SelectItem value="SOCIAL">Social</SelectItem>
                        <SelectItem value="CUSTOM_URL">Custom URL</SelectItem>
                      </SelectContent>
                    </Select>
                  )}
                />
              </div>

              {destinationType === "GOOGLE_REVIEW" ? (
                <div className="space-y-2">
                  <Label>Review location</Label>
                  <Controller
                    name="googleReviewLocationUuid"
                    control={control}
                    render={({ field }) => (
                      <Select value={field.value} onValueChange={field.onChange}>
                        <SelectTrigger>
                          <SelectValue placeholder="Select a location" />
                        </SelectTrigger>
                        <SelectContent>
                          {reviewLocations?.map((loc) => (
                            <SelectItem key={loc.uuid} value={loc.uuid}>
                              {loc.businessName}
                              {loc.locationName ? ` - ${loc.locationName}` : ""}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    )}
                  />
                  {errors.googleReviewLocationUuid && (
                    <p className="text-sm text-destructive">{errors.googleReviewLocationUuid.message}</p>
                  )}
                  {!reviewLocations?.length && (
                    <p className="text-xs text-muted-foreground">
                      No Google Review locations yet - add one on the Google Reviews page first.
                    </p>
                  )}
                </div>
              ) : (
                <div className="space-y-2">
                  <Label htmlFor="externalUrl">URL</Label>
                  <Input
                    id="externalUrl"
                    placeholder="https://..."
                    aria-invalid={Boolean(errors.externalUrl)}
                    {...register("externalUrl")}
                  />
                  {errors.externalUrl && <p className="text-sm text-destructive">{errors.externalUrl.message}</p>}
                </div>
              )}
            </div>

            <DialogFooter className="mt-6">
              <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>
                Cancel
              </Button>
              <Button type="submit" disabled={isSubmitting || createMutation.isPending}>
                {(isSubmitting || createMutation.isPending) && <Loader2 className="size-4 animate-spin" />}
                Create
              </Button>
            </DialogFooter>
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
}
