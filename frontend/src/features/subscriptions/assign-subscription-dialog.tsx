"use client";

import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
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
import { subscriptionAssignSchema, type SubscriptionAssignFormValues } from "@/lib/schemas/commerce";
import { subscriptionsApi } from "@/lib/api/subscriptions";
import { packagesApi } from "@/lib/api/packages";
import { ApiClientError } from "@/lib/api/client";
import type { Subscription } from "@/lib/types/commerce";

export function AssignSubscriptionDialog({
  clientUuid,
  existing,
}: {
  clientUuid: string;
  existing?: Subscription | null;
}) {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();

  const { data: plans } = useQuery({
    queryKey: ["packages"],
    queryFn: () => packagesApi.listAdmin(),
    enabled: open,
  });

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors, isSubmitting },
  } = useForm<SubscriptionAssignFormValues>({
    resolver: zodResolver(subscriptionAssignSchema),
    defaultValues: existing
      ? { packagePlanUuid: existing.plan.uuid, status: existing.status, notes: existing.notes ?? "" }
      : { status: "ACTIVE" },
  });

  const mutation = useMutation({
    mutationFn: (values: SubscriptionAssignFormValues) => subscriptionsApi.assign(clientUuid, values),
    onSuccess: () => {
      toast.success(existing ? "Subscription updated" : "Subscription assigned");
      queryClient.invalidateQueries({ queryKey: ["subscription", clientUuid] });
      setOpen(false);
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to save subscription");
    },
  });

  const handleOpenChange = (next: boolean) => {
    setOpen(next);
    if (!next) reset();
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <Button variant="outline" size="sm" onClick={() => setOpen(true)}>
        {existing ? "Change plan" : "Assign plan"}
      </Button>
      <DialogContent>
        <form onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
          <DialogHeader>
            <DialogTitle>{existing ? "Change subscription" : "Assign subscription"}</DialogTitle>
            <DialogDescription>
              Card and feature limits from the plan are enforced automatically.
            </DialogDescription>
          </DialogHeader>

          <div className="mt-4 space-y-4">
            <div className="space-y-2">
              <Label>Plan</Label>
              <Controller
                name="packagePlanUuid"
                control={control}
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger aria-invalid={Boolean(errors.packagePlanUuid)}>
                      <SelectValue placeholder="Select a plan" />
                    </SelectTrigger>
                    <SelectContent>
                      {plans?.map((plan) => (
                        <SelectItem key={plan.uuid} value={plan.uuid}>
                          {plan.name} - Rs {plan.price.toFixed(2)}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
              {errors.packagePlanUuid && (
                <p className="text-sm text-destructive">{errors.packagePlanUuid.message}</p>
              )}
            </div>
            <div className="space-y-2">
              <Label>Status</Label>
              <Controller
                name="status"
                control={control}
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="TRIAL">Trial</SelectItem>
                      <SelectItem value="ACTIVE">Active</SelectItem>
                      <SelectItem value="EXPIRED">Expired</SelectItem>
                      <SelectItem value="SUSPENDED">Suspended</SelectItem>
                      <SelectItem value="CANCELLED">Cancelled</SelectItem>
                    </SelectContent>
                  </Select>
                )}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="notes">Notes (optional)</Label>
              <Textarea id="notes" rows={2} {...register("notes")} />
            </div>
          </div>

          <DialogFooter className="mt-6">
            <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting || mutation.isPending}>
              {(isSubmitting || mutation.isPending) && <Loader2 className="size-4 animate-spin" />}
              Save
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
