import type { CSSProperties } from "react";

/**
 * Monochrome accent for public-facing dark surfaces - overrides the brand tokens so every
 * var(--primary)/bg-primary/text-primary/bg-accent/ring-ring reference (buttons, links, icons,
 * glows, hover states, focus rings) renders black-and-white instead of the app's gold brand
 * color. Shared by the public marketing layout and the auth layout (login/forgot-password/
 * reset-password) so a visitor going from the home page's "Client Login" link straight into
 * sign-in doesn't hit a jarring brand-color switch. Admin/client dashboards (light theme,
 * never wrapped in `.dark`) keep the gold brand untouched.
 */
export const MONOCHROME_ACCENT = {
  "--primary": "#f5f5f5",
  "--primary-foreground": "#0a0a0a",
  "--accent": "#262626",
  "--accent-foreground": "#f5f5f5",
  "--ring": "#f5f5f5",
} as CSSProperties;
