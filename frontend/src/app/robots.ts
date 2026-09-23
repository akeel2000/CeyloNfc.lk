import type { MetadataRoute } from "next";

import { brand } from "@/lib/config/brand";

export default function robots(): MetadataRoute.Robots {
  return {
    rules: [
      { userAgent: "*", allow: "/", disallow: ["/admin", "/client"] },
    ],
    sitemap: `${brand.domain}/sitemap.xml`,
  };
}
