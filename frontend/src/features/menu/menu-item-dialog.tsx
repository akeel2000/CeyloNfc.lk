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
import { ImageUploadField } from "@/components/media/image-upload-field";
import { menuItemSchema, type MenuItemFormValues } from "@/lib/schemas/menu";
import { menuApi, adminMenuApi } from "@/lib/api/menu";
import { ApiClientError } from "@/lib/api/client";
import type { MenuItem } from "@/lib/types/menu";

export function MenuItemDialog({
  categoryUuid,
  existing,
  clientUuid,
}: {
  categoryUuid: string;
  existing?: MenuItem;
  clientUuid?: string;
}) {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();
  const isEdit = Boolean(existing);

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors, isSubmitting },
  } = useForm<MenuItemFormValues>({
    resolver: zodResolver(menuItemSchema),
    defaultValues: existing
      ? {
          name: existing.name,
          description: existing.description ?? "",
          image: existing.image ?? "",
          price: existing.price,
          available: existing.available,
          featured: existing.featured,
        }
      : { available: true, featured: false },
  });

  const mutation = useMutation({
    mutationFn: (values: MenuItemFormValues) => {
      if (clientUuid) {
        return isEdit
          ? adminMenuApi.updateItem(clientUuid, categoryUuid, existing!.uuid, values)
          : adminMenuApi.createItem(clientUuid, categoryUuid, values);
      }
      return isEdit
        ? menuApi.updateItem(categoryUuid, existing!.uuid, values)
        : menuApi.createItem(categoryUuid, values);
    },
    onSuccess: () => {
      toast.success(isEdit ? "Item updated" : "Item added");
      queryClient.invalidateQueries({ queryKey: clientUuid ? ["admin", "menu", clientUuid] : ["client", "menu"] });
      setOpen(false);
      reset();
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to save item");
    },
  });

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      {isEdit ? (
        <Button variant="ghost" size="icon" onClick={() => setOpen(true)}>
          <Pencil className="size-3.5" />
        </Button>
      ) : (
        <Button variant="outline" size="sm" onClick={() => setOpen(true)}>
          <Plus className="size-4" />
          Add Item
        </Button>
      )}
      <DialogContent>
        <form onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
          <DialogHeader>
            <DialogTitle>{isEdit ? "Edit item" : "Add item"}</DialogTitle>
            <DialogDescription>Shown to customers on your public menu.</DialogDescription>
          </DialogHeader>

          <div className="mt-4 grid gap-4">
            <Controller
              name="image"
              control={control}
              render={({ field }) => (
                <ImageUploadField
                  label="Photo"
                  value={field.value}
                  onChange={field.onChange}
                  category="MENU_ITEM"
                  aspect="wide"
                />
              )}
            />
            <div className="space-y-2">
              <Label htmlFor="name">Name</Label>
              <Input id="name" aria-invalid={Boolean(errors.name)} {...register("name")} />
              {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
            </div>
            <div className="space-y-2">
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
            <div className="flex gap-6">
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" className="size-4" {...register("available")} />
                Available
              </label>
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" className="size-4" {...register("featured")} />
                Featured
              </label>
            </div>
          </div>

          <DialogFooter className="mt-6">
            <Button type="button" variant="outline" onClick={() => setOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting || mutation.isPending}>
              {(isSubmitting || mutation.isPending) && <Loader2 className="size-4 animate-spin" />}
              {isEdit ? "Save changes" : "Add item"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
