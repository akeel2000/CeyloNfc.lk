"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Loader2, Plus } from "lucide-react";
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
import { reviewLocationSchema, type ReviewLocationFormValues } from "@/lib/schemas/review";
import { reviewLocationsApi } from "@/lib/api/review";
import { ApiClientError } from "@/lib/api/client";
import type { GoogleReviewLocation } from "@/lib/types/review";

export function ReviewLocationDialog({ existing }: { existing?: GoogleReviewLocation }) {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();
  const isEdit = Boolean(existing);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ReviewLocationFormValues>({
    resolver: zodResolver(reviewLocationSchema),
    defaultValues: existing
      ? {
          businessName: existing.businessName,
          locationName: existing.locationName ?? "",
          address: existing.address ?? "",
          googleMapsUrl: existing.googleMapsUrl ?? "",
          googleReviewUrl: existing.googleReviewUrl,
          googlePlaceId: existing.googlePlaceId ?? "",
        }
      : undefined,
  });

  const mutation = useMutation({
    mutationFn: (values: ReviewLocationFormValues) =>
      isEdit ? reviewLocationsApi.update(existing!.uuid, values) : reviewLocationsApi.create(values),
    onSuccess: () => {
      toast.success(isEdit ? "Location updated" : "Location added");
      queryClient.invalidateQueries({ queryKey: ["client", "google-reviews"] });
      setOpen(false);
      reset();
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to save location");
    },
  });

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      {isEdit ? (
        <Button variant="outline" size="sm" onClick={() => setOpen(true)}>
          Edit
        </Button>
      ) : (
        <Button onClick={() => setOpen(true)}>
          <Plus className="size-4" />
          Add Location
        </Button>
      )}
      <DialogContent>
        <form onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
          <DialogHeader>
            <DialogTitle>{isEdit ? "Edit location" : "Add Google Review location"}</DialogTitle>
            <DialogDescription>
              Customers who tap or scan will be sent straight to this Google Review link.
            </DialogDescription>
          </DialogHeader>

          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="businessName">Business name</Label>
              <Input id="businessName" aria-invalid={Boolean(errors.businessName)} {...register("businessName")} />
              {errors.businessName && <p className="text-sm text-destructive">{errors.businessName.message}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="locationName">Branch / location name</Label>
              <Input id="locationName" placeholder="e.g. Colombo Branch" {...register("locationName")} />
            </div>
            <div className="space-y-2 sm:col-span-2">
              <Label htmlFor="address">Address</Label>
              <Input id="address" {...register("address")} />
            </div>
            <div className="space-y-2 sm:col-span-2">
              <Label htmlFor="googleReviewUrl">Google Review link</Label>
              <Input
                id="googleReviewUrl"
                placeholder="https://g.page/r/.../review"
                aria-invalid={Boolean(errors.googleReviewUrl)}
                {...register("googleReviewUrl")}
              />
              {errors.googleReviewUrl && (
                <p className="text-sm text-destructive">{errors.googleReviewUrl.message}</p>
              )}
            </div>
            <div className="space-y-2 sm:col-span-2">
              <Label htmlFor="googleMapsUrl">Google Maps link (optional)</Label>
              <Input id="googleMapsUrl" {...register("googleMapsUrl")} />
            </div>
          </div>

          <DialogFooter className="mt-6">
            <Button type="button" variant="outline" onClick={() => setOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting || mutation.isPending}>
              {(isSubmitting || mutation.isPending) && <Loader2 className="size-4 animate-spin" />}
              {isEdit ? "Save changes" : "Add location"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
