"use client";

import { useState } from "react";
import { Controller, useFieldArray, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Loader2, Plus, Trash2 } from "lucide-react";
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
import { orderCreateSchema, type OrderCreateFormValues } from "@/lib/schemas/commerce";
import { ordersApi } from "@/lib/api/orders";
import { clientsApi } from "@/lib/api/clients";
import { productsApi } from "@/lib/api/products";
import { ApiClientError } from "@/lib/api/client";

export function CreateOrderDialog() {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();

  const { data: clients } = useQuery({
    queryKey: ["clients", "for-order"],
    queryFn: () => clientsApi.list({ size: 100 }),
    enabled: open,
  });
  const { data: products } = useQuery({
    queryKey: ["products", "for-order"],
    queryFn: () => productsApi.list(),
    enabled: open,
  });

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors, isSubmitting },
  } = useForm<OrderCreateFormValues>({
    resolver: zodResolver(orderCreateSchema),
    defaultValues: { clientUuid: "", items: [{ productUuid: "", quantity: 1 }], notes: "" },
  });

  const { fields, append, remove } = useFieldArray({ control, name: "items" });

  const mutation = useMutation({
    mutationFn: (values: OrderCreateFormValues) => ordersApi.create(values),
    onSuccess: () => {
      toast.success("Order created");
      queryClient.invalidateQueries({ queryKey: ["orders"] });
      setOpen(false);
      reset({ clientUuid: "", items: [{ productUuid: "", quantity: 1 }], notes: "" });
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to create order");
    },
  });

  const handleOpenChange = (next: boolean) => {
    setOpen(next);
    if (!next) reset({ clientUuid: "", items: [{ productUuid: "", quantity: 1 }], notes: "" });
  };

  const activeProducts = products?.filter((p) => p.active) ?? [];

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <Button onClick={() => setOpen(true)}>
        <Plus className="size-4" />
        Create Order
      </Button>
      <DialogContent className="max-w-xl">
        <form onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
          <DialogHeader>
            <DialogTitle>Create order</DialogTitle>
            <DialogDescription>Line-item totals are computed from current product prices.</DialogDescription>
          </DialogHeader>

          <div className="mt-4 space-y-4">
            <div className="space-y-2">
              <Label>Client</Label>
              <Controller
                name="clientUuid"
                control={control}
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger aria-invalid={Boolean(errors.clientUuid)}>
                      <SelectValue placeholder="Select a client" />
                    </SelectTrigger>
                    <SelectContent>
                      {clients?.content.map((client) => (
                        <SelectItem key={client.uuid} value={client.uuid}>
                          {client.displayName}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
              {errors.clientUuid && <p className="text-sm text-destructive">{errors.clientUuid.message}</p>}
            </div>

            <div className="space-y-2">
              <Label>Items</Label>
              <div className="space-y-3">
                {fields.map((field, index) => (
                  <div key={field.id} className="flex items-start gap-2">
                    <div className="flex-1">
                      <Controller
                        name={`items.${index}.productUuid`}
                        control={control}
                        render={({ field: selectField }) => (
                          <Select value={selectField.value} onValueChange={selectField.onChange}>
                            <SelectTrigger aria-invalid={Boolean(errors.items?.[index]?.productUuid)}>
                              <SelectValue placeholder="Select a product" />
                            </SelectTrigger>
                            <SelectContent>
                              {activeProducts.map((product) => (
                                <SelectItem key={product.uuid} value={product.uuid}>
                                  {product.name} - Rs {product.price.toFixed(2)}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        )}
                      />
                    </div>
                    <Input
                      type="number"
                      min="1"
                      className="w-20"
                      aria-invalid={Boolean(errors.items?.[index]?.quantity)}
                      {...register(`items.${index}.quantity`, { valueAsNumber: true })}
                    />
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      onClick={() => remove(index)}
                      disabled={fields.length === 1}
                    >
                      <Trash2 className="size-4" />
                    </Button>
                  </div>
                ))}
              </div>
              {errors.items?.message && <p className="text-sm text-destructive">{errors.items.message}</p>}
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => append({ productUuid: "", quantity: 1 })}
              >
                <Plus className="size-4" />
                Add item
              </Button>
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
              Create Order
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
