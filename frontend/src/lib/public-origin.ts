/**
 * The public origin (e.g. https://ceylonfc.lk) a visitor actually used. Behind a reverse
 * proxy, request.url in a Route Handler carries the container's internal host
 * (http://<container-id>:3000), so redirects built from it send phones to an unreachable
 * address. nginx forwards Host and X-Forwarded-Proto; prefer those.
 */
export function publicOrigin(request: Request): string {
  const url = new URL(request.url);
  const host = request.headers.get("x-forwarded-host") ?? request.headers.get("host") ?? url.host;
  const proto = request.headers.get("x-forwarded-proto")?.split(",")[0].trim() ?? url.protocol.replace(":", "");
  return `${proto}://${host}`;
}
