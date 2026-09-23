"use client";

import { useState } from "react";
import { Download, Nfc, Pause, Play } from "lucide-react";

export function DemoPreview() {
  const [paused, setPaused] = useState(false);

  return (
    <div className="mx-auto mt-12 max-w-xl">
      <div className="landing-demo glass-panel relative overflow-hidden rounded-[22px]" data-paused={paused}>
        <div className="flex items-center gap-2.5 border-b border-white/10 px-[18px] py-3.5">
          <span className="landing-rec-dot size-2 rounded-full bg-destructive" />
          <span className="text-[11px] font-medium uppercase tracking-[0.1em] text-white/55">Live tap preview</span>
        </div>

        <div className="relative flex h-[280px] items-center justify-center overflow-hidden bg-[radial-gradient(circle_at_50%_30%,rgba(255,255,255,0.05),transparent_65%)] sm:h-[340px]">
          <div className="landing-demo-card-move absolute bottom-[64%] left-1/2 flex h-11 w-[70px] -translate-x-1/2 items-center justify-center rounded-lg border border-white/20 shadow-[0_10px_24px_-10px_rgba(0,0,0,0.6)]" style={{ background: "linear-gradient(160deg, #1f1f1f, #0a0a0a)" }}>
            <Nfc className="size-4" style={{ color: "var(--primary)" }} />
          </div>
          <div className="landing-demo-ripple absolute bottom-2 left-1/2 size-[60px] -translate-x-1/2 rounded-full border-2" style={{ borderColor: "var(--primary)" }} />

          <div className="relative h-[290px] w-[150px] overflow-hidden rounded-[26px] border border-white/[0.14] bg-[#0c0c14] shadow-[0_30px_60px_-20px_rgba(0,0,0,0.7)]">
            <div className="absolute inset-2 overflow-hidden rounded-[20px] bg-[#08080d]">
              <div className="landing-demo-idle absolute inset-0 flex flex-col items-center justify-center gap-2.5 p-4 text-center">
                <Nfc className="size-[30px] opacity-80" style={{ color: "var(--primary)" }} />
                <span className="font-mono text-[10px] tracking-[0.14em] text-white/40">READY TO TAP</span>
              </div>
              <div className="landing-demo-profile absolute inset-0 flex flex-col items-center justify-center gap-2.5 p-4 text-center">
                <div
                  className="flex size-[46px] items-center justify-center rounded-full text-[15px] font-semibold text-white"
                  style={{ background: "linear-gradient(135deg, #52525b, #18181b)" }}
                >
                  AP
                </div>
                <strong className="text-xs font-semibold text-white">Alex Perera</strong>
                <small className="text-[10px] text-white/50">Sales Manager &middot; CeyloNfc</small>
                <span
                  className="mt-1 inline-flex items-center gap-1.5 rounded-full px-3.5 py-1.5 text-[10px] font-semibold"
                  style={{ background: "var(--primary)", color: "var(--primary-foreground)" }}
                >
                  <Download className="size-[11px]" />
                  Save Contact
                </span>
              </div>
            </div>
          </div>
        </div>

        <div className="relative h-[3px] bg-white/[0.08]">
          <div className="landing-demo-progress-fill absolute inset-0" style={{ background: "var(--primary)" }} />
        </div>

        <button
          type="button"
          onClick={() => setPaused((v) => !v)}
          aria-label={paused ? "Play preview" : "Pause preview"}
          className="absolute bottom-[18px] left-[18px] z-10 flex size-[38px] items-center justify-center rounded-full border border-white/20 bg-black/50 text-white backdrop-blur-md"
        >
          {paused ? <Play className="size-4" /> : <Pause className="size-4" />}
        </button>
      </div>
      <p className="mt-5 text-center text-sm text-muted-foreground">
        Illustrative preview &mdash; &quot;Alex Perera&quot; is a sample profile, not a real customer.
      </p>
    </div>
  );
}
