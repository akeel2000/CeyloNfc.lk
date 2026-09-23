"use client";

import { useRef, useState } from "react";
import { ImagePlus, Loader2, X } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { mediaApi, type MediaCategory } from "@/lib/api/media";
import { ApiClientError } from "@/lib/api/client";

const MAX_SIZE_BYTES = 5 * 1024 * 1024;
const ACCEPTED_TYPES = ["image/jpeg", "image/png", "image/webp", "image/gif"];

export function ImageUploadField({
  label,
  value,
  onChange,
  category,
  aspect = "square",
}: {
  label: string;
  value: string | null | undefined;
  onChange: (url: string) => void;
  category: MediaCategory;
  aspect?: "square" | "wide";
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
      const result = await mediaApi.upload(file, category);
      onChange(result.url);
      toast.success("Image uploaded");
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to upload image");
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div className="space-y-2">
      <Label>{label}</Label>
      <div className="flex items-center gap-3">
        <div
          className={
            aspect === "square"
              ? "flex size-16 shrink-0 items-center justify-center overflow-hidden rounded-md border border-border bg-secondary/50"
              : "flex h-16 w-28 shrink-0 items-center justify-center overflow-hidden rounded-md border border-border bg-secondary/50"
          }
        >
          {value ? (
            // eslint-disable-next-line @next/next/no-img-element -- external, unpredictable-origin uploaded images; next/image would require configuring a remote pattern per deployment domain
            <img src={value} alt="" className="size-full object-cover" />
          ) : (
            <ImagePlus className="size-5 text-muted-foreground" />
          )}
        </div>
        <div className="flex flex-col gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={isUploading}
            onClick={() => inputRef.current?.click()}
          >
            {isUploading && <Loader2 className="size-4 animate-spin" />}
            {value ? "Replace" : "Upload"}
          </Button>
          {value && (
            <Button type="button" variant="ghost" size="sm" onClick={() => onChange("")}>
              <X className="size-3.5" />
              Remove
            </Button>
          )}
        </div>
        <input
          ref={inputRef}
          type="file"
          accept={ACCEPTED_TYPES.join(",")}
          className="hidden"
          onChange={handleFileChange}
        />
      </div>
    </div>
  );
}
