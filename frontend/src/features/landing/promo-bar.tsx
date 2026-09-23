"use client";

import { useEffect, useState } from "react";
import { X } from "lucide-react";

const DISMISSED_KEY = "ceylonfc-promo-dismissed";

export function PromoBar() {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    // Starts hidden on both server and first client render (localStorage doesn't exist on
    // the server) to avoid a hydration mismatch, then reveals once we can read the real
    // dismissed state - an intentional exception to the usual "no setState in effect" rule.
    /* eslint-disable react-hooks/set-state-in-effect */
    try {
      if (localStorage.getItem(DISMISSED_KEY) !== "1") setVisible(true);
    } catch {
      setVisible(true);
    }
    /* eslint-enable react-hooks/set-state-in-effect */
  }, []);

  if (!visible) return null;

  return (
    <div
      className="relative flex flex-wrap items-center justify-center gap-2.5 overflow-hidden px-[46px] py-2.5 text-center text-[13px] font-medium text-primary-foreground"
      style={{ background: "linear-gradient(90deg, var(--primary), #d4d4d4)" }}
    >
      <div className="landing-sheen pointer-events-none absolute inset-0" style={{ background: "linear-gradient(100deg, transparent, rgba(255,255,255,0.35), transparent)" }} />
      <span>
        <strong className="font-bold">[YOUR OFFER HERE]</strong> on every NFC card this month.{" "}
        <a href="#contact" className="font-semibold underline underline-offset-2">
          Claim now &rarr;
        </a>
      </span>
      <button
        type="button"
        aria-label="Dismiss offer"
        onClick={() => {
          setVisible(false);
          try {
            localStorage.setItem(DISMISSED_KEY, "1");
          } catch {
            // localStorage unavailable (private mode, blocked) - dismissal just won't persist
          }
        }}
        className="absolute right-2.5 top-1/2 flex size-[26px] -translate-y-1/2 items-center justify-center rounded-full bg-black/15"
      >
        <X className="size-[13px]" />
      </button>
    </div>
  );
}
