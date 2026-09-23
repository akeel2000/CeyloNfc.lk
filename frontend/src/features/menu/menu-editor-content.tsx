"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ExternalLink, GripVertical, Loader2, Eye, EyeOff, Trash2, UtensilsCrossed } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { PageHeader } from "@/components/layout/page-header";
import { menuApi, adminMenuApi } from "@/lib/api/menu";
import { ApiClientError } from "@/lib/api/client";
import { formatCurrency } from "@/lib/format";
import { menuUpdateSchema, type MenuUpdateFormValues } from "@/lib/schemas/menu";
import { AddCategoryDialog } from "@/features/menu/add-category-dialog";
import { MenuItemDialog } from "@/features/menu/menu-item-dialog";
import type { Menu, MenuCategory, MenuItem } from "@/lib/types/menu";

function moveItem<T>(list: T[], from: number, to: number): T[] {
  const next = [...list];
  const [moved] = next.splice(from, 1);
  next.splice(to, 0, moved);
  return next;
}

export function MenuEditorContent({ clientUuid }: { clientUuid?: string } = {}) {
  const { data: menu, isLoading } = useQuery({
    queryKey: clientUuid ? ["admin", "menu", clientUuid] : ["client", "menu"],
    queryFn: () => (clientUuid ? adminMenuApi.get(clientUuid) : menuApi.getOwn()),
  });

  if (isLoading || !menu) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  return <MenuEditor menu={menu} clientUuid={clientUuid} />;
}

function MenuEditor({ menu, clientUuid }: { menu: Menu; clientUuid?: string }) {
  const queryClient = useQueryClient();
  const queryKey = clientUuid ? ["admin", "menu", clientUuid] : ["client", "menu"];
  const invalidate = () => queryClient.invalidateQueries({ queryKey });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<MenuUpdateFormValues>({
    resolver: zodResolver(menuUpdateSchema),
    defaultValues: {
      slug: menu.slug,
      name: menu.name,
      description: menu.description ?? "",
      logo: menu.logo ?? "",
      currency: menu.currency,
    },
  });

  useEffect(() => {
    reset({
      slug: menu.slug,
      name: menu.name,
      description: menu.description ?? "",
      logo: menu.logo ?? "",
      currency: menu.currency,
    });
  }, [menu, reset]);

  const updateMutation = useMutation({
    mutationFn: (values: MenuUpdateFormValues) =>
      clientUuid ? adminMenuApi.update(clientUuid, values) : menuApi.update(values),
    onSuccess: () => {
      toast.success("Menu saved");
      invalidate();
    },
    onError: (error) => toast.error(error instanceof ApiClientError ? error.message : "Failed to save menu"),
  });

  const publishMutation = useMutation({
    mutationFn: (published: boolean) =>
      clientUuid ? adminMenuApi.setPublished(clientUuid, published) : menuApi.setPublished(published),
    onSuccess: (updated) => {
      toast.success(updated.published ? "Menu published" : "Menu unpublished");
      invalidate();
    },
    onError: (error) => toast.error(error instanceof ApiClientError ? error.message : "Failed to update"),
  });

  const deleteCategoryMutation = useMutation({
    mutationFn: (categoryUuid: string) =>
      clientUuid ? adminMenuApi.deleteCategory(clientUuid, categoryUuid) : menuApi.deleteCategory(categoryUuid),
    onSuccess: () => {
      toast.success("Category deleted");
      invalidate();
    },
    onError: (error) => toast.error(error instanceof ApiClientError ? error.message : "Failed to delete category"),
  });

  const deleteItemMutation = useMutation({
    mutationFn: ({ categoryUuid, itemUuid }: { categoryUuid: string; itemUuid: string }) =>
      clientUuid
        ? adminMenuApi.deleteItem(clientUuid, categoryUuid, itemUuid)
        : menuApi.deleteItem(categoryUuid, itemUuid),
    onSuccess: () => {
      toast.success("Item deleted");
      invalidate();
    },
    onError: (error) => toast.error(error instanceof ApiClientError ? error.message : "Failed to delete item"),
  });

  // Only the categories/items whose position actually changed are sent - a drag that shifts
  // one card past two others otherwise touched every row in between for no reason.
  const reorderCategoriesMutation = useMutation({
    mutationFn: (ordered: MenuCategory[]) =>
      Promise.all(
        ordered
          .map((category, index) => ({ category, index }))
          .filter(({ category, index }) => category.sortOrder !== index)
          .map(({ category, index }) =>
            clientUuid
              ? adminMenuApi.updateCategory(clientUuid, category.uuid, {
                  name: category.name,
                  active: category.active,
                  sortOrder: index,
                })
              : menuApi.updateCategory(category.uuid, { name: category.name, active: category.active, sortOrder: index })
          )
      ),
    onSuccess: invalidate,
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to reorder categories");
      invalidate();
    },
  });

  const reorderItemsMutation = useMutation({
    mutationFn: ({ categoryUuid, ordered }: { categoryUuid: string; ordered: MenuItem[] }) =>
      Promise.all(
        ordered
          .map((item, index) => ({ item, index }))
          .filter(({ item, index }) => item.sortOrder !== index)
          .map(({ item, index }) => {
            const payload = {
              name: item.name,
              description: item.description ?? undefined,
              image: item.image ?? undefined,
              price: item.price,
              available: item.available,
              featured: item.featured,
              sortOrder: index,
            };
            return clientUuid
              ? adminMenuApi.updateItem(clientUuid, categoryUuid, item.uuid, payload)
              : menuApi.updateItem(categoryUuid, item.uuid, payload);
          })
      ),
    onSuccess: invalidate,
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to reorder items");
      invalidate();
    },
  });

  const [draggedCategory, setDraggedCategory] = useState<number | null>(null);
  const [draggedItem, setDraggedItem] = useState<{ categoryUuid: string; index: number } | null>(null);

  const dropCategory = (targetIndex: number) => {
    if (draggedCategory === null || draggedCategory === targetIndex) {
      setDraggedCategory(null);
      return;
    }
    reorderCategoriesMutation.mutate(moveItem(menu.categories, draggedCategory, targetIndex));
    setDraggedCategory(null);
  };

  const dropItem = (categoryUuid: string, items: MenuItem[], targetIndex: number) => {
    if (!draggedItem || draggedItem.categoryUuid !== categoryUuid || draggedItem.index === targetIndex) {
      setDraggedItem(null);
      return;
    }
    reorderItemsMutation.mutate({ categoryUuid, ordered: moveItem(items, draggedItem.index, targetIndex) });
    setDraggedItem(null);
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Menu"
        titleExtra={
          <div className="flex items-center gap-2">
            <Badge variant={menu.published ? "success" : "secondary"}>
              {menu.published ? "Published" : "Draft"}
            </Badge>
            {menu.published && (
              <a
                href={menu.publicUrl}
                target="_blank"
                rel="noreferrer"
                className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
              >
                {menu.publicUrl.replace(/^https?:\/\//, "")}
                <ExternalLink className="size-3" />
              </a>
            )}
          </div>
        }
        actions={
          <Button
            variant={menu.published ? "outline" : "default"}
            onClick={() => publishMutation.mutate(!menu.published)}
            disabled={publishMutation.isPending}
          >
            {publishMutation.isPending ? (
              <Loader2 className="size-4 animate-spin" />
            ) : menu.published ? (
              <EyeOff className="size-4" />
            ) : (
              <Eye className="size-4" />
            )}
            {menu.published ? "Unpublish" : "Publish"}
          </Button>
        }
      />

      <Card>
        <CardHeader>
          <CardTitle>Menu settings</CardTitle>
          <CardDescription>Public URL: /menu/{menu.slug}</CardDescription>
        </CardHeader>
        <CardContent>
          <form
            onSubmit={handleSubmit((values) => updateMutation.mutate(values))}
            className="grid gap-4 sm:grid-cols-2"
            noValidate
          >
            <div className="space-y-2">
              <Label htmlFor="name">Menu name</Label>
              <Input id="name" aria-invalid={Boolean(errors.name)} {...register("name")} />
              {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="slug">URL slug</Label>
              <Input id="slug" aria-invalid={Boolean(errors.slug)} {...register("slug")} />
              {errors.slug && <p className="text-sm text-destructive">{errors.slug.message}</p>}
            </div>
            <div className="space-y-2 sm:col-span-2">
              <Label htmlFor="description">Description</Label>
              <Textarea id="description" rows={2} {...register("description")} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="currency">Currency</Label>
              <Input id="currency" placeholder={formatCurrency(0)} {...register("currency")} />
            </div>
            <div className="flex items-end">
              <Button type="submit" disabled={!isDirty || updateMutation.isPending}>
                {updateMutation.isPending && <Loader2 className="size-4 animate-spin" />}
                Save changes
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">Categories</h2>
        <AddCategoryDialog clientUuid={clientUuid} />
      </div>

      {menu.categories.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center gap-2 py-16 text-center">
            <UtensilsCrossed className="size-8 text-muted-foreground" />
            <p className="text-sm font-medium">No categories yet</p>
            <p className="max-w-xs text-sm text-muted-foreground">
              Add a category like &quot;Starters&quot; or &quot;Drinks&quot; to start building this menu.
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {menu.categories.map((category, categoryIndex) => (
            <Card
              key={category.uuid}
              onDragOver={(e) => e.preventDefault()}
              onDrop={() => dropCategory(categoryIndex)}
              className={draggedCategory === categoryIndex ? "opacity-50" : undefined}
            >
              <CardHeader className="flex flex-row items-center justify-between space-y-0">
                <div className="flex items-center gap-2">
                  <span
                    draggable
                    onDragStart={() => setDraggedCategory(categoryIndex)}
                    onDragEnd={() => setDraggedCategory(null)}
                    className="cursor-grab text-muted-foreground hover:text-foreground active:cursor-grabbing"
                    aria-label="Drag to reorder category"
                  >
                    <GripVertical className="size-4" />
                  </span>
                  <CardTitle className="text-base">{category.name}</CardTitle>
                </div>
                <div className="flex items-center gap-2">
                  <MenuItemDialog categoryUuid={category.uuid} clientUuid={clientUuid} />
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => deleteCategoryMutation.mutate(category.uuid)}
                    disabled={deleteCategoryMutation.isPending}
                  >
                    <Trash2 className="size-4" />
                  </Button>
                </div>
              </CardHeader>
              <CardContent>
                {category.items.length === 0 ? (
                  <p className="text-sm text-muted-foreground">No items in this category yet.</p>
                ) : (
                  <ul className="divide-y divide-border">
                    {category.items.map((item, itemIndex) => (
                      <li
                        key={item.uuid}
                        onDragOver={(e) => e.preventDefault()}
                        onDrop={() => dropItem(category.uuid, category.items, itemIndex)}
                        className={`flex items-center justify-between gap-3 py-2 ${
                          draggedItem?.categoryUuid === category.uuid && draggedItem.index === itemIndex
                            ? "opacity-50"
                            : ""
                        }`}
                      >
                        <span
                          draggable
                          onDragStart={() => setDraggedItem({ categoryUuid: category.uuid, index: itemIndex })}
                          onDragEnd={() => setDraggedItem(null)}
                          className="cursor-grab text-muted-foreground hover:text-foreground active:cursor-grabbing"
                          aria-label="Drag to reorder item"
                        >
                          <GripVertical className="size-4" />
                        </span>
                        <div className="flex-1">
                          <p className="font-medium">
                            {item.name}
                            {!item.available && (
                              <span className="ml-2 text-xs text-muted-foreground">(unavailable)</span>
                            )}
                            {item.featured && (
                              <Badge variant="warning" className="ml-2 align-middle">
                                Featured
                              </Badge>
                            )}
                          </p>
                          {item.description && (
                            <p className="text-sm text-muted-foreground">{item.description}</p>
                          )}
                        </div>
                        <div className="flex items-center gap-2">
                          <span className="font-medium">
                            {menu.currency} {item.price.toFixed(2)}
                          </span>
                          <MenuItemDialog categoryUuid={category.uuid} existing={item} clientUuid={clientUuid} />
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() =>
                              deleteItemMutation.mutate({ categoryUuid: category.uuid, itemUuid: item.uuid })
                            }
                            disabled={deleteItemMutation.isPending}
                          >
                            <Trash2 className="size-3.5" />
                          </Button>
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
