"use client";

import { useState } from "react";
import { Controller, useForm, useWatch } from "react-hook-form";
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
import { ImageUploadField } from "@/components/media/image-upload-field";
import { templateSchema, type TemplateFormValues } from "@/lib/schemas/template";
import { templatesApi } from "@/lib/api/templates";
import { ApiClientError } from "@/lib/api/client";
import type { Template } from "@/lib/types/template";

export function TemplateDialog({ existing }: { existing?: Template }) {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();
  const isEdit = Boolean(existing);

  const {
    register,
    handleSubmit,
    reset,
    control,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<TemplateFormValues>({
    resolver: zodResolver(templateSchema),
    defaultValues: existing
      ? {
          name: existing.name,
          description: existing.description ?? "",
          previewImage: existing.previewImage ?? "",
          primaryColor: existing.primaryColor,
          layout: existing.layout,
          premium: existing.premium,
          active: existing.active,
          sortOrder: existing.sortOrder,
        }
      : {
          primaryColor: "#4338ca",
          layout: "CLASSIC",
          premium: false,
          active: true,
          sortOrder: 0,
        },
  });

  const previewImage = useWatch({ control, name: "previewImage" });
  const primaryColor = useWatch({ control, name: "primaryColor" });

  const mutation = useMutation({
    mutationFn: (values: TemplateFormValues) =>
      isEdit ? templatesApi.update(existing!.uuid, values) : templatesApi.create(values),
    onSuccess: () => {
      toast.success(isEdit ? "Template updated" : "Template created");
      queryClient.invalidateQueries({ queryKey: ["templates"] });
      setOpen(false);
      reset();
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to save template");
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
          Create Template
        </Button>
      )}
      <DialogContent className="max-w-lg">
        <form onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
          <DialogHeader>
            <DialogTitle>{isEdit ? "Edit template" : "Create template"}</DialogTitle>
            <DialogDescription>Shown in the client gallery for selecting a profile look.</DialogDescription>
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
            <div className="sm:col-span-2">
              <ImageUploadField
                label="Preview image"
                value={previewImage}
                onChange={(url) => setValue("previewImage", url, { shouldDirty: true })}
                category="TEMPLATE_PREVIEW"
                aspect="wide"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="primaryColor">Primary color</Label>
              <div className="flex items-center gap-2">
                <input
                  type="color"
                  className="size-9 shrink-0 rounded-md border border-border"
                  value={/^#[0-9A-Fa-f]{6}$/.test(primaryColor ?? "") ? primaryColor : "#4338ca"}
                  onChange={(e) => setValue("primaryColor", e.target.value, { shouldDirty: true })}
                />
                <Input
                  id="primaryColor"
                  aria-invalid={Boolean(errors.primaryColor)}
                  {...register("primaryColor")}
                />
              </div>
              {errors.primaryColor && <p className="text-sm text-destructive">{errors.primaryColor.message}</p>}
            </div>
            <div className="space-y-2">
              <Label>Layout</Label>
              <Controller
                name="layout"
                control={control}
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="CLASSIC">Classic</SelectItem>
                      <SelectItem value="MINIMAL">Minimal</SelectItem>
                    </SelectContent>
                  </Select>
                )}
              />
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
                <input type="checkbox" className="size-4" {...register("premium")} />
                Premium
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
              {isEdit ? "Save changes" : "Create Template"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
