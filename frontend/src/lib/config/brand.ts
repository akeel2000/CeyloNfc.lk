/**
 * Central brand configuration. Components should import from here rather than
 * hardcoding the platform name/domain, so a rebrand is a one-file change.
 * Once the Settings module (Phase 9) lands, Super Admin-configurable values
 * here should move to the backend `settings` table instead of this file.
 */
export const brand = {
  name: "CeyloNfc",
  domain: process.env.NEXT_PUBLIC_SITE_URL ?? "https://ceylonfc.com",
  supportEmail: "noormohommaduakeel@gmail.com",
  tagline: "One Tap. One Connection. Unlimited Possibilities.",
} as const;
