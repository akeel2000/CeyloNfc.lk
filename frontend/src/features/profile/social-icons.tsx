import type { ComponentType, SVGProps } from "react";
import { Camera, Globe, Link2, MessageCircle, Music2, PlayCircle, Send, X as XGlyph } from "lucide-react";

import type { SocialPlatform } from "@/lib/types/profile";

type IconComponent = ComponentType<SVGProps<SVGSVGElement>>;

/**
 * lucide-react (this app's only icon dependency) dropped brand/logo glyphs for trademark
 * reasons, so platforms without a reasonable generic lucide stand-in (Facebook's "f", LinkedIn's
 * "in") get a hand-drawn letterform here instead of pulling in a whole icon-pack dependency for
 * two glyphs.
 */
function FacebookGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" {...props}>
      <text
        x="12"
        y="12.5"
        textAnchor="middle"
        dominantBaseline="central"
        fontSize="16"
        fontWeight="700"
        fontFamily="system-ui, sans-serif"
        fill="currentColor"
      >
        f
      </text>
    </svg>
  );
}

function LinkedInGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" {...props}>
      <text
        x="12"
        y="12.5"
        textAnchor="middle"
        dominantBaseline="central"
        fontSize="10"
        fontWeight="700"
        fontFamily="system-ui, sans-serif"
        fill="currentColor"
      >
        in
      </text>
    </svg>
  );
}

export interface SocialMeta {
  label: string;
  Icon: IconComponent;
  /** Soft brand-evocative tint for the glass button's glow/border on hover - purely decorative. */
  tint: string;
}

export const SOCIAL_META: Record<SocialPlatform, SocialMeta> = {
  FACEBOOK: { label: "Facebook", Icon: FacebookGlyph, tint: "#1877F2" },
  INSTAGRAM: { label: "Instagram", Icon: Camera, tint: "#E1306C" },
  LINKEDIN: { label: "LinkedIn", Icon: LinkedInGlyph, tint: "#0A66C2" },
  TIKTOK: { label: "TikTok", Icon: Music2, tint: "#25F4EE" },
  YOUTUBE: { label: "YouTube", Icon: PlayCircle, tint: "#FF0000" },
  X: { label: "X", Icon: XGlyph, tint: "#e7e9ea" },
  TELEGRAM: { label: "Telegram", Icon: Send, tint: "#26A5E4" },
  WHATSAPP: { label: "WhatsApp", Icon: MessageCircle, tint: "#25D366" },
  CUSTOM: { label: "Link", Icon: Link2, tint: "#a3a3a3" },
};

export const WEBSITE_META: SocialMeta = { label: "Website", Icon: Globe, tint: "#818cf8" };
