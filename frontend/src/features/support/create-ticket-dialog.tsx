"use client";

import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Loader2, Plus } from "lucide-react";
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
import { AttachmentPicker } from "@/features/support/attachment-picker";
import { ticketCreateSchema, type TicketCreateFormValues } from "@/lib/schemas/support";
import { supportApi } from "@/lib/api/support";
import { ApiClientError } from "@/lib/api/client";

export function CreateTicketDialog() {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors, isSubmitting },
  } = useForm<TicketCreateFormValues>({
    resolver: zodResolver(ticketCreateSchema),
    defaultValues: { priority: "MEDIUM" },
  });
  const [attachmentUrl, setAttachmentUrl] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: (values: TicketCreateFormValues) =>
      supportApi.createOwn({ ...values, attachmentUrl: attachmentUrl ?? undefined }),
    onSuccess: () => {
      toast.success("Support ticket created");
      queryClient.invalidateQueries({ queryKey: ["support", "own"] });
      setOpen(false);
      reset({ priority: "MEDIUM" });
      setAttachmentUrl(null);
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to create ticket");
    },
  });

  const handleOpenChange = (next: boolean) => {
    setOpen(next);
    if (!next) {
      reset({ priority: "MEDIUM" });
      setAttachmentUrl(null);
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <Button onClick={() => setOpen(true)}>
        <Plus className="size-4" />
        New Ticket
      </Button>
      <DialogContent>
        <form onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
          <DialogHeader>
            <DialogTitle>New support ticket</DialogTitle>
            <DialogDescription>Our team typically replies within one business day.</DialogDescription>
          </DialogHeader>

          <div className="mt-4 space-y-4">
            <div className="space-y-2">
              <Label htmlFor="subject">Subject</Label>
              <Input id="subject" aria-invalid={Boolean(errors.subject)} {...register("subject")} />
              {errors.subject && <p className="text-sm text-destructive">{errors.subject.message}</p>}
            </div>
            <div className="space-y-2">
              <Label>Priority</Label>
              <Controller
                name="priority"
                control={control}
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="LOW">Low</SelectItem>
                      <SelectItem value="MEDIUM">Medium</SelectItem>
                      <SelectItem value="HIGH">High</SelectItem>
                    </SelectContent>
                  </Select>
                )}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="message">Message</Label>
              <Textarea id="message" rows={4} aria-invalid={Boolean(errors.message)} {...register("message")} />
              {errors.message && <p className="text-sm text-destructive">{errors.message.message}</p>}
            </div>
            <div className="flex items-center gap-2">
              <AttachmentPicker value={attachmentUrl} onChange={setAttachmentUrl} />
              {!attachmentUrl && <span className="text-xs text-muted-foreground">Attach a screenshot (optional)</span>}
            </div>
          </div>

          <DialogFooter className="mt-6">
            <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting || mutation.isPending}>
              {(isSubmitting || mutation.isPending) && <Loader2 className="size-4 animate-spin" />}
              Create Ticket
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
