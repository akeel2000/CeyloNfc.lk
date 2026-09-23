import type { LucideIcon } from "lucide-react";
import { Nfc } from "lucide-react";

/**
 * A premium, credit-card-proportioned mockup - same visual language as the hero's NFC card
 * (top bar icon + wordmark, faint oversized watermark icon, bottom bar chip + label) so every
 * card design on the site reads as one consistent physical product line, not a generic icon tile.
 */
export function CardMockup({ icon: Icon, label, background }: { icon: LucideIcon; label: string; background: string }) {
  return (
    <div
      className="relative overflow-hidden rounded-[20px] border border-white/10 shadow-[0_20px_50px_-20px_rgba(0,0,0,0.7)]"
      style={{ aspectRatio: "1.586", background }}
    >
      <div
        className="landing-sheen absolute -left-[30%] -top-[60%] h-[220%] w-[55%]"
        style={{ background: "linear-gradient(100deg, transparent, rgba(255,255,255,0.22), transparent)", transform: "translateX(-40%) rotate(18deg)", mixBlendMode: "screen" }}
      />
      <div className="absolute inset-x-4 top-4 flex items-center justify-between text-white/85">
        <Nfc className="size-4" />
        <span className="text-[11px] font-semibold tracking-tight">CeyloNfc</span>
      </div>
      <Icon className="absolute -bottom-3 -right-3 size-20 text-white/[0.12]" strokeWidth={1} />
      <div className="absolute inset-x-4 bottom-4 flex items-end justify-between">
        <Icon className="size-6 text-white/85" />
        <span className="font-mono text-[9px] tracking-[0.12em] text-white/60">{label}</span>
      </div>
    </div>
  );
}
