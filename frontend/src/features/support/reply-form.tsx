"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2, Send } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { AttachmentPicker } from "@/features/support/attachment-picker";
import { messageCreateSchema, type MessageCreateFormValues } from "@/lib/schemas/support";

export function ReplyForm({
  onSubmit,
  isPending,
}: {
  onSubmit: (values: MessageCreateFormValues & { attachmentUrl?: string }) => void;
  isPending: boolean;
}) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<MessageCreateFormValues>({ resolver: zodResolver(messageCreateSchema) });
  const [attachmentUrl, setAttachmentUrl] = useState<string | null>(null);

  return (
    <form
      onSubmit={handleSubmit((values) => {
        onSubmit({ ...values, attachmentUrl: attachmentUrl ?? undefined });
        reset();
        setAttachmentUrl(null);
      })}
      noValidate
      className="space-y-2"
    >
      <div className="flex items-start gap-2">
        <div className="flex-1">
          <Textarea rows={2} placeholder="Write a reply..." aria-invalid={Boolean(errors.body)} {...register("body")} />
          {errors.body && <p className="mt-1 text-sm text-destructive">{errors.body.message}</p>}
        </div>
        <AttachmentPicker value={attachmentUrl} onChange={setAttachmentUrl} />
        <Button type="submit" size="icon" disabled={isSubmitting || isPending}>
          {isSubmitting || isPending ? <Loader2 className="size-4 animate-spin" /> : <Send className="size-4" />}
        </Button>
      </div>
    </form>
  );
}
