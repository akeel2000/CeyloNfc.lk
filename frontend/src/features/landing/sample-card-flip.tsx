import { Nfc } from "lucide-react";

import { brand } from "@/lib/config/brand";
import { QrImage } from "@/features/qr/qr-image";

/**
 * A realistic front/back mockup of a personalized card - front carries the logo, brand name,
 * and cardholder's name/title; back carries the real QR fallback for phones without NFC.
 * "Alex Perera" is the same illustrative sample identity used in the tap-to-connect demo
 * elsewhere on the site - not a real customer.
 */
export function SampleCardFlip() {
  return (
    <div className="mx-auto grid max-w-2xl gap-8 sm:grid-cols-2">
      <div>
        <div
          className="relative overflow-hidden rounded-[20px] border border-white/10 shadow-[0_20px_50px_-20px_rgba(0,0,0,0.7)]"
          style={{ aspectRatio: "1.586", background: "linear-gradient(155deg, #4338ca 0%, #4f46e5 45%, #1e1b4b 100%)" }}
        >
          <div
            className="landing-sheen absolute -left-[30%] -top-[60%] h-[220%] w-[55%]"
            style={{ background: "linear-gradient(100deg, transparent, rgba(255,255,255,0.22), transparent)", transform: "translateX(-40%) rotate(18deg)", mixBlendMode: "screen" }}
          />
          <div className="absolute inset-x-5 top-5 flex items-center gap-2">
            <span className="flex size-7 items-center justify-center rounded-md bg-white/15">
              <Nfc className="size-4 text-white" />
            </span>
            <span className="text-sm font-semibold tracking-tight text-white">{brand.name}</span>
          </div>
          <div className="absolute inset-x-5 bottom-5">
            <p className="text-lg font-semibold text-white">Alex Perera</p>
            <p className="text-xs text-white/70">Sales Manager</p>
          </div>
        </div>
        <p className="mt-3 text-center text-xs font-medium uppercase tracking-[0.12em] text-muted-foreground">Front</p>
      </div>

      <div>
        <div
          className="relative flex items-center justify-center overflow-hidden rounded-[20px] border border-white/10 shadow-[0_20px_50px_-20px_rgba(0,0,0,0.7)]"
          style={{ aspectRatio: "1.586", background: "linear-gradient(155deg, #1e1b4b 0%, #17153b 55%, #0a081f 100%)" }}
        >
          <div className="flex flex-col items-center gap-2">
            <div className="rounded-lg bg-white p-1.5">
              <QrImage value={`${brand.domain}/t/sample`} size={78} />
            </div>
            <span className="text-[9px] font-medium uppercase tracking-[0.14em] text-white/60">Scan or tap to connect</span>
          </div>
        </div>
        <p className="mt-3 text-center text-xs font-medium uppercase tracking-[0.12em] text-muted-foreground">Back</p>
      </div>

      <p className="sm:col-span-2 text-center text-sm text-muted-foreground">
        Illustrative preview - &quot;Alex Perera&quot; is a sample profile, not a real customer.
      </p>
    </div>
  );
}
