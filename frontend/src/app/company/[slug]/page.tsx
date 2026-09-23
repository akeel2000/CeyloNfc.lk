import type { Metadata } from "next";
import { notFound } from "next/navigation";

import { env } from "@/lib/config/env";
import { ViewBeacon } from "@/components/analytics/view-beacon";
import { PublicProfileCard } from "@/features/profile/public-profile-card";
import type { ApiResponse } from "@/lib/types/api";
import type { PublicProfile } from "@/lib/types/profile";

async function fetchProfile(slug: string): Promise<PublicProfile | null> {
  const response = await fetch(`${env.apiUrl}/public/company/${slug}`, { cache: "no-store" });
  if (!response.ok) return null;
  const json = (await response.json()) as ApiResponse<PublicProfile>;
  return json.success ? json.data : null;
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ slug: string }>;
}): Promise<Metadata> {
  const { slug } = await params;
  const profile = await fetchProfile(slug);
  if (!profile) return { title: "Company not found" };
  return {
    title: profile.companyName ?? "Company Profile",
    description: profile.bio ?? undefined,
  };
}

export default async function PublicCompanyProfilePage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  const profile = await fetchProfile(slug);
  if (!profile) notFound();

  return (
    <>
      <ViewBeacon path={`/public/company/${slug}/view`} />
      <PublicProfileCard profile={profile} vcardPath={`/public/company/${slug}/vcard`} />
    </>
  );
}
