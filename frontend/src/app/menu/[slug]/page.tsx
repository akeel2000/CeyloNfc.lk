import type { Metadata } from "next";
import { notFound } from "next/navigation";

import { env } from "@/lib/config/env";
import { ViewBeacon } from "@/components/analytics/view-beacon";
import { PublicMenuView } from "@/features/menu/public-menu-view";
import type { ApiResponse } from "@/lib/types/api";
import type { Menu } from "@/lib/types/menu";

async function fetchMenu(slug: string): Promise<Menu | null> {
  const response = await fetch(`${env.apiUrl}/public/menu/${slug}`, { cache: "no-store" });
  if (!response.ok) return null;
  const json = (await response.json()) as ApiResponse<Menu>;
  return json.success ? json.data : null;
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ slug: string }>;
}): Promise<Metadata> {
  const { slug } = await params;
  const menu = await fetchMenu(slug);
  if (!menu) return { title: "Menu not found" };
  return { title: menu.name, description: menu.description ?? undefined };
}

export default async function PublicMenuPage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  const menu = await fetchMenu(slug);
  if (!menu) notFound();

  return (
    <>
      <ViewBeacon path={`/public/menu/${slug}/view`} />
      <PublicMenuView menu={menu} />
    </>
  );
}
