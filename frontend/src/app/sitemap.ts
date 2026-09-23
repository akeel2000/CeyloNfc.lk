import type { MetadataRoute } from "next";

import { brand } from "@/lib/config/brand";

export default function sitemap(): MetadataRoute.Sitemap {
  const base = brand.domain;

  return [
    { url: `${base}/`, changeFrequency: "weekly", priority: 1 },
    { url: `${base}/privacy`, changeFrequency: "yearly", priority: 0.2 },
    { url: `${base}/terms`, changeFrequency: "yearly", priority: 0.2 },
  ];
}
