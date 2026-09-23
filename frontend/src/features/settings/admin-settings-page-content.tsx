"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { DetailSkeleton } from "@/components/ui/page-skeleton";
import { PageHeader } from "@/components/layout/page-header";
import { adminSettingsApi } from "@/lib/api/settings";
import { ApiClientError } from "@/lib/api/client";
import { platformSettingsSchema, type PlatformSettingsFormValues } from "@/lib/schemas/settings";
import type { PlatformSettings } from "@/lib/types/settings";

export function AdminSettingsPageContent() {
  const { data: settings, isLoading } = useQuery({
    queryKey: ["admin", "settings"],
    queryFn: () => adminSettingsApi.get(),
  });

  if (isLoading || !settings) {
    return <DetailSkeleton />;
  }

  return <SettingsForm settings={settings} />;
}

function SettingsForm({ settings }: { settings: PlatformSettings }) {
  const queryClient = useQueryClient();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<PlatformSettingsFormValues>({
    resolver: zodResolver(platformSettingsSchema),
    defaultValues: {
      siteName: settings.siteName,
      supportEmail: settings.supportEmail,
      tagline: settings.tagline ?? "",
    },
  });

  useEffect(() => {
    reset({ siteName: settings.siteName, supportEmail: settings.supportEmail, tagline: settings.tagline ?? "" });
  }, [settings, reset]);

  const updateMutation = useMutation({
    mutationFn: (values: PlatformSettingsFormValues) => adminSettingsApi.update(values),
    onSuccess: () => {
      toast.success("Settings saved");
      queryClient.invalidateQueries({ queryKey: ["admin", "settings"] });
      // The public site header/footer read these same values live - see site-header.tsx /
      // site-footer.tsx - so a fresh visit to the public site reflects this change immediately.
      queryClient.invalidateQueries({ queryKey: ["public", "settings"] });
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to save settings");
    },
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Settings"
        description="Platform-wide branding shown across the public site and client dashboard."
      />

      <Card>
        <CardHeader>
          <CardTitle>Branding</CardTitle>
          <CardDescription>
            The site name and tagline appear on the public homepage, header and footer; the support
            email is shown to visitors as the platform contact address.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form
            onSubmit={handleSubmit((values) => updateMutation.mutate(values))}
            className="grid gap-4 sm:grid-cols-2"
            noValidate
          >
            <div className="space-y-2">
              <Label htmlFor="siteName">Site name</Label>
              <Input id="siteName" aria-invalid={Boolean(errors.siteName)} {...register("siteName")} />
              {errors.siteName && <p className="text-sm text-destructive">{errors.siteName.message}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="supportEmail">Support email</Label>
              <Input
                id="supportEmail"
                type="email"
                aria-invalid={Boolean(errors.supportEmail)}
                {...register("supportEmail")}
              />
              {errors.supportEmail && <p className="text-sm text-destructive">{errors.supportEmail.message}</p>}
            </div>
            <div className="space-y-2 sm:col-span-2">
              <Label htmlFor="tagline">Tagline</Label>
              <Textarea id="tagline" rows={2} {...register("tagline")} />
              {errors.tagline && <p className="text-sm text-destructive">{errors.tagline.message}</p>}
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
    </div>
  );
}
