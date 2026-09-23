"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Check, Lock, LayoutTemplate, Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { PageHeader } from "@/components/layout/page-header";
import { templatesApi } from "@/lib/api/templates";
import { ApiClientError } from "@/lib/api/client";
import type { TemplateGalleryItem } from "@/lib/types/template";

function TemplateCard({ item }: { item: TemplateGalleryItem }) {
  const queryClient = useQueryClient();
  const { template, locked, selected } = item;

  const mutation = useMutation({
    mutationFn: () => templatesApi.apply(template.uuid),
    onSuccess: () => {
      toast.success(`"${template.name}" applied to your profile`);
      queryClient.invalidateQueries({ queryKey: ["templates", "gallery"] });
      queryClient.invalidateQueries({ queryKey: ["client", "profile"] });
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to apply template");
    },
  });

  return (
    <Card className={selected ? "ring-2 ring-primary" : undefined}>
      <div
        className="flex h-32 items-center justify-center rounded-t-lg bg-cover bg-center"
        style={
          template.previewImage
            ? { backgroundImage: `url(${template.previewImage})` }
            : { backgroundColor: template.primaryColor }
        }
      >
        {!template.previewImage && <LayoutTemplate className="size-8 text-white/70" />}
      </div>
      <CardContent className="space-y-3 p-4">
        <div className="flex items-start justify-between gap-2">
          <div>
            <p className="font-medium">{template.name}</p>
            {template.description && (
              <p className="text-sm text-muted-foreground">{template.description}</p>
            )}
          </div>
          {template.premium && (
            <Badge variant="warning" className="shrink-0">
              Premium
            </Badge>
          )}
        </div>

        {selected ? (
          <Button className="w-full" disabled variant="secondary">
            <Check className="size-4" />
            Selected
          </Button>
        ) : locked ? (
          <Button className="w-full" disabled variant="outline">
            <Lock className="size-4" />
            Upgrade to unlock
          </Button>
        ) : (
          <Button className="w-full" variant="outline" disabled={mutation.isPending} onClick={() => mutation.mutate()}>
            {mutation.isPending && <Loader2 className="size-4 animate-spin" />}
            Select
          </Button>
        )}
      </CardContent>
    </Card>
  );
}

export function TemplateGallery() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["templates", "gallery"],
    queryFn: () => templatesApi.gallery(),
  });

  return (
    <div className="space-y-6">
      <PageHeader title="Templates" description="Pick a look for your public profile page." />

      {isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-56 w-full" />
          ))}
        </div>
      ) : isError ? (
        <p className="text-sm text-destructive">Failed to load templates.</p>
      ) : !data || data.length === 0 ? (
        <EmptyState
          icon={LayoutTemplate}
          title="No templates available yet"
          description="Check back later for new looks."
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {data.map((item) => (
            <TemplateCard key={item.template.uuid} item={item} />
          ))}
        </div>
      )}
    </div>
  );
}
