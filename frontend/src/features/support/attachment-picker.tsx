"use client";

import { useRef, useState } from "react";
import { Loader2, Paperclip, X } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { mediaApi } from "@/lib/api/media";
import { ApiClientError } from "@/lib/api/client";

const MAX_SIZE_BYTES = 5 * 1024 * 1024;
const ACCEPTED_TYPES = ["image/jpeg", "image/png", "image/webp", "image/gif"];

/** Uploads immediately on file selection (same as ImageUploadField) rather than deferring
 *  upload to message-send time - the message body is a single `POST` with an already-hosted
 *  URL, not a multipart form, so the attachment has to exist server-side first either way. */
export function AttachmentPicker({
  value,
  onChange,
}: {
  value: string | null;
  onChange: (url: string | null) => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [isUploading, setIsUploading] = useState(false);

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;

    if (!ACCEPTED_TYPES.includes(file.type)) {
      toast.error("Only JPEG, PNG, WEBP or GIF images are supported");
      return;
    }
    if (file.size > MAX_SIZE_BYTES) {
      toast.error("File exceeds the 5MB size limit");
      return;
    }

    setIsUploading(true);
    try {
      const result = await mediaApi.upload(file, "TICKET_ATTACHMENT");
      onChange(result.url);
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to upload attachment");
    } finally {
      setIsUploading(false);
    }
  };

  if (value) {
    return (
      <div className="flex items-center gap-1.5 rounded-md border border-border bg-secondary/50 px-2 py-1.5 text-xs">
        <Paperclip className="size-3.5 text-muted-foreground" />
        <span>Image attached</span>
        <button
          type="button"
          onClick={() => onChange(null)}
          className="text-muted-foreground hover:text-foreground"
          aria-label="Remove attachment"
        >
          <X className="size-3.5" />
        </button>
      </div>
    );
  }

  return (
    <>
      <Button
        type="button"
        variant="ghost"
        size="icon"
        disabled={isUploading}
        onClick={() => inputRef.current?.click()}
        title="Attach an image"
      >
        {isUploading ? <Loader2 className="size-4 animate-spin" /> : <Paperclip className="size-4" />}
      </Button>
      <input
        ref={inputRef}
        type="file"
        accept={ACCEPTED_TYPES.join(",")}
        className="hidden"
        onChange={handleFileChange}
      />
    </>
  );
}
