import { Building2 } from "lucide-react";

import { cn } from "@/lib/utils";

/**
 * The "2D" profile style - same anatomy as the real public profile page's classic layout
 * (cover band, avatar overlapping the bottom edge, name/title below; see
 * features/profile/public-profile-card.tsx), themed by this look's own accent color. Shares the
 * same premium background treatment as TemplateLook3D (particle field + soft color glow) so the
 * two styles read as one consistent design system, not an older/plainer fallback.
 */
export function TemplateLook2D({
  color,
  name = "Alex Perera",
  title = "Sales Manager",
  isIndividual = true,
  compact = false,
}: {
  color: string;
  name?: string;
  title?: string;
  isIndividual?: boolean;
  compact?: boolean;
}) {
  const initials = name
    .split(" ")
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();

  return (
    <div className="relative w-full overflow-hidden rounded-2xl bg-[#0a0a0a]">
      <div className="hero-particles pointer-events-none absolute inset-0 opacity-25" />
      <div
        className="pointer-events-none absolute left-1/2 top-0 size-32 -translate-x-1/2 rounded-full opacity-30 blur-3xl"
        style={{ background: color }}
      />

      <div className={cn("relative", compact ? "h-12" : "h-16")} style={{ background: `linear-gradient(135deg, ${color}, ${color}66)` }} />
      <div className={cn("relative flex flex-col items-center", compact ? "px-3 pb-4" : "px-4 pb-5")}>
        <div
          className={cn(
            "flex items-center justify-center border-4 border-[#0a0a0a] font-semibold text-white shadow-[0_10px_25px_-8px_rgba(0,0,0,0.7)]",
            compact ? "-mt-6 size-12 text-sm" : "-mt-8 size-16 text-base",
            isIndividual ? "rounded-full" : "rounded-xl"
          )}
          style={{ background: color }}
        >
          {isIndividual ? initials : <Building2 className={compact ? "size-5" : "size-6"} />}
        </div>
        <p className={cn("mt-2 text-center font-semibold text-white", compact ? "text-sm" : "text-base")}>{name}</p>
        <p className={cn("text-center text-white/50", compact ? "text-[11px]" : "text-xs")}>{title}</p>
      </div>
    </div>
  );
}
