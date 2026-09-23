"use client";

import { useEffect } from "react";
import Link from "next/link";
import { Controller, useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ExternalLink, LayoutTemplate, Loader2, Eye, EyeOff } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { ImageUploadField } from "@/components/media/image-upload-field";
import { PageHeader } from "@/components/layout/page-header";
import { profileApi, adminProfileApi } from "@/lib/api/profile";
import { ApiClientError } from "@/lib/api/client";
import { profileUpdateSchema, type ProfileUpdateFormValues } from "@/lib/schemas/profile";
import { PhonePreview } from "@/features/profile/phone-preview";
import { SocialLinksEditor } from "@/features/profile/social-links-editor";
import { BusinessHoursEditor } from "@/features/profile/business-hours-editor";
import type { Profile } from "@/lib/types/profile";

export function ProfileEditorContent({ clientUuid }: { clientUuid?: string } = {}) {
  const { data: profile, isLoading } = useQuery({
    queryKey: clientUuid ? ["admin", "profile", clientUuid] : ["client", "profile"],
    queryFn: () => (clientUuid ? adminProfileApi.get(clientUuid) : profileApi.getOwn()),
  });

  if (isLoading || !profile) {
    return (
      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        <Skeleton className="h-96 w-full" />
        <Skeleton className="hidden h-96 w-full lg:block" />
      </div>
    );
  }

  return <ProfileEditor profile={profile} clientUuid={clientUuid} />;
}

function ProfileEditor({ profile, clientUuid }: { profile: Profile; clientUuid?: string }) {
  const queryClient = useQueryClient();
  const isIndividual = profile.type === "INDIVIDUAL";
  const queryKey = clientUuid ? ["admin", "profile", clientUuid] : ["client", "profile"];

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors, isDirty },
  } = useForm<ProfileUpdateFormValues>({
    resolver: zodResolver(profileUpdateSchema),
    defaultValues: profileToFormValues(profile),
  });

  useEffect(() => {
    reset(profileToFormValues(profile));
  }, [profile, reset]);

  const watched = useWatch({ control });

  const updateMutation = useMutation({
    mutationFn: (values: ProfileUpdateFormValues) =>
      clientUuid ? adminProfileApi.update(clientUuid, values) : profileApi.update(values),
    onSuccess: () => {
      toast.success("Profile saved");
      queryClient.invalidateQueries({ queryKey });
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to save profile");
    },
  });

  const publishMutation = useMutation({
    mutationFn: (published: boolean) =>
      clientUuid ? adminProfileApi.setPublished(clientUuid, published) : profileApi.setPublished(published),
    onSuccess: (updated) => {
      toast.success(updated.published ? "Profile published" : "Profile unpublished");
      queryClient.invalidateQueries({ queryKey });
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to update publish status");
    },
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title={clientUuid ? (isIndividual ? "Profile" : "Company Profile") : isIndividual ? "My Profile" : "My Company"}
        titleExtra={
          <div className="flex items-center gap-2">
            <Badge variant={profile.published ? "success" : "secondary"}>
              {profile.published ? "Published" : "Draft"}
            </Badge>
            {profile.published && (
              <a
                href={profile.publicUrl}
                target="_blank"
                rel="noreferrer"
                className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
              >
                {profile.publicUrl.replace(/^https?:\/\//, "")}
                <ExternalLink className="size-3" />
              </a>
            )}
          </div>
        }
        actions={
          <>
            {!clientUuid && (
              <Button variant="outline" asChild>
                <Link href="/client/templates">
                  <LayoutTemplate className="size-4" />
                  {profile.templateUuid ? "Change template" : "Choose template"}
                </Link>
              </Button>
            )}
            <Button
              variant={profile.published ? "outline" : "default"}
              onClick={() => publishMutation.mutate(!profile.published)}
              disabled={publishMutation.isPending}
            >
              {publishMutation.isPending ? (
                <Loader2 className="size-4 animate-spin" />
              ) : profile.published ? (
                <EyeOff className="size-4" />
              ) : (
                <Eye className="size-4" />
              )}
              {profile.published ? "Unpublish" : "Publish"}
            </Button>
          </>
        }
      />

      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Basic information</CardTitle>
              <CardDescription>
                Your public URL:{" "}
                <Link href={`/${isIndividual ? "p" : "company"}/${watched.slug || profile.slug}`} className="underline">
                  /{isIndividual ? "p" : "company"}/{watched.slug || profile.slug}
                </Link>
              </CardDescription>
            </CardHeader>
            <CardContent>
              <form
                onSubmit={handleSubmit((values) => updateMutation.mutate(values))}
                className="grid gap-4 sm:grid-cols-2"
                noValidate
              >
                <div className="space-y-2 sm:col-span-2">
                  <Label htmlFor="slug">URL slug</Label>
                  <Input id="slug" aria-invalid={Boolean(errors.slug)} {...register("slug")} />
                  {errors.slug && <p className="text-sm text-destructive">{errors.slug.message}</p>}
                </div>

                <div className="flex flex-wrap gap-6 sm:col-span-2">
                  {isIndividual ? (
                    <Controller
                      name="profileImage"
                      control={control}
                      render={({ field }) => (
                        <ImageUploadField
                          label="Profile photo"
                          value={field.value}
                          onChange={field.onChange}
                          category="PROFILE_PHOTO"
                        />
                      )}
                    />
                  ) : (
                    <Controller
                      name="logo"
                      control={control}
                      render={({ field }) => (
                        <ImageUploadField
                          label="Company logo"
                          value={field.value}
                          onChange={field.onChange}
                          category="COMPANY_LOGO"
                        />
                      )}
                    />
                  )}
                  <Controller
                    name="coverImage"
                    control={control}
                    render={({ field }) => (
                      <ImageUploadField
                        label="Cover image"
                        value={field.value}
                        onChange={field.onChange}
                        category="PROFILE_COVER"
                        aspect="wide"
                      />
                    )}
                  />
                </div>

                {isIndividual ? (
                  <>
                    <div className="space-y-2">
                      <Label htmlFor="fullName">Full name</Label>
                      <Input id="fullName" {...register("fullName")} />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="jobTitle">Job title</Label>
                      <Input id="jobTitle" {...register("jobTitle")} />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="companyName">Company</Label>
                      <Input id="companyName" {...register("companyName")} />
                    </div>
                  </>
                ) : (
                  <>
                    <div className="space-y-2">
                      <Label htmlFor="companyName">Company name</Label>
                      <Input id="companyName" {...register("companyName")} />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="industry">Industry</Label>
                      <Input id="industry" {...register("industry")} />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="registrationNumber">Registration number</Label>
                      <Input id="registrationNumber" {...register("registrationNumber")} />
                    </div>
                  </>
                )}

                <div className="space-y-2 sm:col-span-2">
                  <Label htmlFor="bio">{isIndividual ? "Bio" : "Description"}</Label>
                  <Textarea id="bio" rows={3} {...register("bio")} />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="phone">Phone</Label>
                  <Input id="phone" {...register("phone")} />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="whatsapp">WhatsApp</Label>
                  <Input id="whatsapp" {...register("whatsapp")} />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="email">Email</Label>
                  <Input id="email" type="email" aria-invalid={Boolean(errors.email)} {...register("email")} />
                  {errors.email && <p className="text-sm text-destructive">{errors.email.message}</p>}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="website">Website</Label>
                  <Input id="website" {...register("website")} />
                </div>
                <div className="space-y-2 sm:col-span-2">
                  <Label htmlFor="address">Address</Label>
                  <Input id="address" {...register("address")} />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="city">City</Label>
                  <Input id="city" {...register("city")} />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="country">Country</Label>
                  <Input id="country" {...register("country")} />
                </div>

                {!isIndividual && (
                  <div className="space-y-2 sm:col-span-2">
                    <Label htmlFor="googleMapsUrl">Google Maps URL</Label>
                    <Input id="googleMapsUrl" {...register("googleMapsUrl")} />
                  </div>
                )}

                <div className="sm:col-span-2">
                  <Button type="submit" disabled={!isDirty || updateMutation.isPending}>
                    {updateMutation.isPending && <Loader2 className="size-4 animate-spin" />}
                    Save changes
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>

          <SocialLinksEditor initialLinks={profile.socialLinks} clientUuid={clientUuid} />

          {!isIndividual && (
            <BusinessHoursEditor initialHours={profile.businessHours} clientUuid={clientUuid} />
          )}
        </div>

        <div className="hidden lg:block">
          <div className="sticky top-6">
            <p className="mb-3 text-center text-xs font-medium uppercase tracking-wide text-muted-foreground">
              Live preview
            </p>
            <PhonePreview type={profile.type} values={watched} socialLinks={profile.socialLinks} />
          </div>
        </div>
      </div>
    </div>
  );
}

function profileToFormValues(profile: Profile): ProfileUpdateFormValues {
  return {
    slug: profile.slug,
    fullName: profile.fullName ?? "",
    jobTitle: profile.jobTitle ?? "",
    companyName: profile.companyName ?? "",
    industry: profile.industry ?? "",
    registrationNumber: profile.registrationNumber ?? "",
    googleMapsUrl: profile.googleMapsUrl ?? "",
    logo: profile.logo ?? "",
    bio: profile.bio ?? "",
    profileImage: profile.profileImage ?? "",
    coverImage: profile.coverImage ?? "",
    phone: profile.phone ?? "",
    whatsapp: profile.whatsapp ?? "",
    email: profile.email ?? "",
    website: profile.website ?? "",
    address: profile.address ?? "",
    city: profile.city ?? "",
    country: profile.country ?? "",
  };
}
