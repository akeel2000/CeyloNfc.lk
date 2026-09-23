"use client";

import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Loader2, Plus, Pencil } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { packagePlanSchema, toPackagePlanPayload, type PackagePlanFormValues } from "@/lib/schemas/commerce";
import { packagesApi } from "@/lib/api/packages";
import { ApiClientError } from "@/lib/api/client";
import type { PackagePlan } from "@/lib/types/commerce";

export function PackagePlanDialog({ existing }: { existing?: PackagePlan }) {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();
  const isEdit = Boolean(existing);

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors, isSubmitting },
  } = useForm<PackagePlanFormValues>({
    resolver: zodResolver(packagePlanSchema),
    defaultValues: existing
      ? {
          name: existing.name,
          description: existing.description ?? "",
          price: existing.price,
          billingPeriod: existing.billingPeriod,
          cardLimit: existing.cardLimit ?? undefined,
          profileLimit: existing.profileLimit ?? undefined,
          reviewLocationLimit: existing.reviewLocationLimit ?? undefined,
          menuLimit: existing.menuLimit ?? undefined,
          premiumTemplates: existing.premiumTemplates,
          active: existing.active,
          sortOrder: existing.sortOrder,
        }
      : {
          billingPeriod: "MONTHLY",
          premiumTemplates: false,
          active: true,
          sortOrder: 0,
        },
  });

  const mutation = useMutation({
    mutationFn: (values: PackagePlanFormValues) => {
      const payload = toPackagePlanPayload(values);
      return isEdit ? packagesApi.update(existing!.uuid, payload) : packagesApi.create(payload);
    },
    onSuccess: () => {
      toast.success(isEdit ? "Package updated" : "Package created");
      queryClient.invalidateQueries({ queryKey: ["packages"] });
      setOpen(false);
      reset();
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to save package");
    },
  });

  const handleOpenChange = (next: boolean) => {
    setOpen(next);
    if (!next) reset();
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      {isEdit ? (
        <Button variant="ghost" size="icon" onClick={() => setOpen(true)}>
          <Pencil className="size-3.5" />
        </Button>
      ) : (
        <Button onClick={() => setOpen(true)}>
          <Plus className="size-4" />
          Create Package
        </Button>
      )}
      <DialogContent className="max-w-lg">
        <form onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
          <DialogHeader>
            <DialogTitle>{isEdit ? "Edit package" : "Create package"}</DialogTitle>
            <DialogDescription>
              Drives the public pricing page and client card/feature limits.
            </DialogDescription>
          </DialogHeader>

          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <div className="space-y-2 sm:col-span-2">
              <Label htmlFor="name">Name</Label>
              <Input id="name" aria-invalid={Boolean(errors.name)} {...register("name")} />
              {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
            </div>
            <div className="space-y-2 sm:col-span-2">
              <Label htmlFor="description">Description</Label>
              <Textarea id="description" rows={2} {...register("description")} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="price">Price</Label>
              <Input
                id="price"
                type="number"
                step="0.01"
                min="0"
                aria-invalid={Boolean(errors.price)}
                {...register("price", { valueAsNumber: true })}
              />
              {errors.price && <p className="text-sm text-destructive">{errors.price.message}</p>}
            </div>
            <div className="space-y-2">
              <Label>Billing period</Label>
              <Controller
                name="billingPeriod"
                control={control}
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="MONTHLY">Monthly</SelectItem>
                      <SelectItem value="YEARLY">Yearly</SelectItem>
                      <SelectItem value="ONE_TIME">One time</SelectItem>
                    </SelectContent>
                  </Select>
                )}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="cardLimit">Card limit (blank = unlimited)</Label>
              <Input
                id="cardLimit"
                type="number"
                min="0"
                {...register("cardLimit", { valueAsNumber: true })}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="profileLimit">Profile limit (blank = unlimited)</Label>
              <Input
                id="profileLimit"
                type="number"
                min="0"
                {...register("profileLimit", { valueAsNumber: true })}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="reviewLocationLimit">Review location limit</Label>
              <Input
                id="reviewLocationLimit"
                type="number"
                min="0"
                {...register("reviewLocationLimit", { valueAsNumber: true })}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="menuLimit">Menu limit</Label>
              <Input id="menuLimit" type="number" min="0" {...register("menuLimit", { valueAsNumber: true })} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="sortOrder">Sort order</Label>
              <Input
                id="sortOrder"
                type="number"
                aria-invalid={Boolean(errors.sortOrder)}
                {...register("sortOrder", { valueAsNumber: true })}
              />
            </div>
            <div className="flex items-end gap-6">
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" className="size-4" {...register("premiumTemplates")} />
                Premium templates
              </label>
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" className="size-4" {...register("active")} />
                Active
              </label>
            </div>
          </div>

          <DialogFooter className="mt-6">
            <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting || mutation.isPending}>
              {(isSubmitting || mutation.isPending) && <Loader2 className="size-4 animate-spin" />}
              {isEdit ? "Save changes" : "Create Package"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
