import { NextResponse } from "next/server";

import { env } from "@/lib/config/env";

/**
 * Physical NFC chips are written with https://<domain>/t/{token}. This route calls the
 * backend's redirect endpoint server-side (manual redirect mode, so we can read the
 * Location header and branch on error status) rather than blindly forwarding the browser
 * to the backend - that way a suspended/unknown card shows a proper error page instead of
 * raw backend JSON. See docs/NFC_FLOW.md.
 *
 * Forwards the visitor's real User-Agent/Referer/IP explicitly - fetch() from a Route
 * Handler does NOT do this automatically, and without it every tap would look like it
 * came from the Next.js server itself (wrong analytics device/browser data, and a shared
 * rate-limit bucket across every real visitor instead of one per visitor).
 */
export async function GET(request: Request, { params }: { params: Promise<{ token: string }> }) {
  const { token } = await params;

  const forwardHeaders: Record<string, string> = {};
  const userAgent = request.headers.get("user-agent");
  const referer = request.headers.get("referer");
  const forwardedFor = request.headers.get("x-forwarded-for");
  if (userAgent) forwardHeaders["User-Agent"] = userAgent;
  if (referer) forwardHeaders["Referer"] = referer;
  if (forwardedFor) forwardHeaders["X-Forwarded-For"] = forwardedFor;

  const backendResponse = await fetch(`${env.apiUrl}/public/t/${encodeURIComponent(token)}`, {
    redirect: "manual",
    cache: "no-store",
    headers: forwardHeaders,
  });

  if (backendResponse.status === 302 || backendResponse.status === 307) {
    const location = backendResponse.headers.get("location");
    if (location) {
      return NextResponse.redirect(location, 302);
    }
  }

  if (backendResponse.status === 410) {
    return NextResponse.redirect(new URL("/card-unavailable?reason=inactive", request.url));
  }
  if (backendResponse.status === 429) {
    return NextResponse.redirect(new URL("/card-unavailable?reason=rate-limited", request.url));
  }

  return NextResponse.redirect(new URL("/card-unavailable?reason=not-found", request.url));
}
