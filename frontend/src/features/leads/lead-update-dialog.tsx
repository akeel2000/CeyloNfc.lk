"use client";

import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Loader2, Pencil } from "lucide-react";
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
import { leadUpdateSchema, type LeadUpdateFormValues } from "@/lib/schemas/lead";
import { leadsApi } from "@/lib/api/leads";
import { ApiClientError } from "@/lib/api/client";
import type { Lead } from "@/lib/types/lead";

export function LeadUpdateDialog({ lead }: { lead: Lead }) {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();

  const {
    handleSubmit,
    reset,
    control,
    register,
    formState: { isSubmitting },
  } = useForm<LeadUpdateFormValues>({
    resolver: zodResolver(leadUpdateSchema),
    defaultValues: { status: lead.status, notes: lead.notes ?? "" },
  });

  const mutation = useMutation({
    mutationFn: (values: LeadUpdateFormValues) => leadsApi.update(lead.uuid, values),
    onSuccess: () => {
      toast.success("Lead updated");
      queryClient.invalidateQueries({ queryKey: ["leads"] });
      setOpen(false);
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to update lead");
    },
  });

  const handleOpenChange = (next: boolean) => {
    setOpen(next);
    if (!next) reset();
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <Button variant="ghost" size="icon" onClick={() => setOpen(true)}>
        <Pencil className="size-3.5" />
      </Button>
      <DialogContent>
        <form onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
          <DialogHeader>
            <DialogTitle>{lead.name}</DialogTitle>
            <DialogDescription>{lead.email}</DialogDescription>
          </DialogHeader>

          <div className="mt-4 space-y-4">
            {lead.message && (
              <div className="rounded-md border border-border bg-secondary/50 p-3 text-sm">{lead.message}</div>
            )}
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
                      <SelectItem value="NEW">New</SelectItem>
                      <SelectItem value="CONTACTED">Contacted</SelectItem>
                      <SelectItem value="CONVERTED">Converted</SelectItem>
                      <SelectItem value="CLOSED">Closed</SelectItem>
                    </SelectContent>
                  </Select>
                )}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="notes">Internal notes</Label>
              <Textarea id="notes" rows={3} {...register("notes")} />
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
