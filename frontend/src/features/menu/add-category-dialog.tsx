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
import { menuCategorySchema, type MenuCategoryFormValues } from "@/lib/schemas/menu";
import { menuApi, adminMenuApi } from "@/lib/api/menu";
import { ApiClientError } from "@/lib/api/client";

export function AddCategoryDialog({ clientUuid }: { clientUuid?: string }) {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<MenuCategoryFormValues>({ resolver: zodResolver(menuCategorySchema) });

  const mutation = useMutation({
    mutationFn: (values: MenuCategoryFormValues) =>
      clientUuid ? adminMenuApi.createCategory(clientUuid, values) : menuApi.createCategory(values),
    onSuccess: () => {
      toast.success("Category added");
      queryClient.invalidateQueries({ queryKey: clientUuid ? ["admin", "menu", clientUuid] : ["client", "menu"] });
      setOpen(false);
      reset();
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to add category");
    },
  });

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <Button variant="outline" onClick={() => setOpen(true)}>
        <Plus className="size-4" />
        Add Category
      </Button>
      <DialogContent>
        <form onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
          <DialogHeader>
            <DialogTitle>Add category</DialogTitle>
            <DialogDescription>e.g. Starters, Main Course, Drinks, Desserts.</DialogDescription>
          </DialogHeader>
          <div className="mt-4 space-y-2">
            <Label htmlFor="name">Category name</Label>
            <Input id="name" aria-invalid={Boolean(errors.name)} {...register("name")} />
            {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
          </div>
          <DialogFooter className="mt-6">
            <Button type="button" variant="outline" onClick={() => setOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting || mutation.isPending}>
              {(isSubmitting || mutation.isPending) && <Loader2 className="size-4 animate-spin" />}
              Add
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
