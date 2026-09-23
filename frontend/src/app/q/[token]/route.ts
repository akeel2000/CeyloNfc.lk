import { NextResponse } from "next/server";

import { env } from "@/lib/config/env";

/**
 * QR scan redirect - identical pattern to /t/[token] (NFC taps), including forwarding the
 * visitor's real User-Agent/Referer/IP (see the comment there for why that matters).
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

  const backendResponse = await fetch(`${env.apiUrl}/public/q/${encodeURIComponent(token)}`, {
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
