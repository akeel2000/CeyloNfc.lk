"use client";

import { useEffect } from "react";

import { env } from "@/lib/config/env";

/**
 * Fires once on mount to record a PROFILE_VIEW/MENU_VIEW analytics event. Deliberately a
 * client-side beacon rather than recorded during the page's own server-side data fetch - the
 * public profile/menu pages are plain fetch() in a Server Component, which would forward
 * Next's own request headers rather than the visitor's real browser User-Agent, corrupting the
 * device/browser analytics breakdown. sendBeacon (falling back to a keepalive fetch) carries
 * the visitor's real headers on the actual wire request with nothing to forward. Result is
 * intentionally ignored - a dropped view-count beacon must never surface to the visitor.
 */
export function ViewBeacon({ path }: { path: string }) {
  useEffect(() => {
    const url = `${env.apiUrl}${path}`;
    if (typeof navigator !== "undefined" && typeof navigator.sendBeacon === "function") {
      navigator.sendBeacon(url);
    } else {
      fetch(url, { method: "POST", keepalive: true }).catch(() => undefined);
    }
  }, [path]);

  return null;
}
