import { Building2 } from "lucide-react";

import { cn } from "@/lib/utils";

/**
 * The same "3D style" visual language as the profile editor's interactive preview
 * (features/profile/phone-preview.tsx ThreeDimensionalPreview): a tilting gradient-framed
 * avatar over a drifting particle field, themed by this look's own accent color rather than the
 * site's brand color (each look demonstrates a different color).
 *
 * Deliberately does NOT use OrbitingSocialRing here - that component enforces a 220px+ minimum
 * width meant for a spacious hero section, which overflows and clips inside a narrow grid
 * thumbnail. The full orbiting version still appears in the expanded dialog (see
 * template-look-card.tsx), which has enough room for it.
 */
export function TemplateLook3D({
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
    <div
      className={cn(
        "relative flex w-full flex-col items-center overflow-hidden rounded-2xl bg-[#0a0a0a]",
        compact ? "px-3 pb-4 pt-6" : "px-4 pb-6 pt-8"
      )}
    >
      <div className="hero-particles absolute inset-0 opacity-25" />
      <div
        className="absolute left-1/2 top-8 size-28 -translate-x-1/2 rounded-full opacity-30 blur-3xl"
        style={{ background: color }}
      />

      <div
        className={cn(
          "editor-3d-avatar relative flex shrink-0 items-center justify-center p-1 shadow-[0_15px_35px_-10px_rgba(0,0,0,0.7)]",
          compact ? "size-16 rounded-2xl" : "size-20 rounded-[1.4rem]"
        )}
        style={{ background: `linear-gradient(135deg, ${color}, ${color}99, ${color}33)` }}
      >
        <div className={cn("absolute inset-[3px] border border-white/25 bg-white/10", compact ? "rounded-xl" : "rounded-[1.1rem]")} />
        {isIndividual ? (
          <span className={cn("relative font-semibold text-white", compact ? "text-sm" : "text-base")}>{initials}</span>
        ) : (
          <Building2 className={cn("relative text-white/90", compact ? "size-5" : "size-6")} />
        )}
      </div>

      <p className={cn("relative mt-2 text-center font-semibold text-white", compact ? "text-sm" : "mt-3 text-base")}>{name}</p>
      <p className={cn("relative text-center text-white/50", compact ? "text-[11px]" : "text-xs")}>{title}</p>
    </div>
  );
}
