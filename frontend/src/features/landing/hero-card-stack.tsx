"use client";

import { useEffect, useRef } from "react";
import { Nfc, Share2, Star, UtensilsCrossed, Cpu } from "lucide-react";

const MINI_CARDS = [
  {
    key: "menu",
    label: "SMART MENU",
    icon: UtensilsCrossed,
    color: "#4ade80",
    background: "linear-gradient(160deg, #052e12 0%, #14532d 55%, #030f09 100%)",
    borderColor: "rgba(74,222,128,0.35)",
    transform: "translate3d(26px, 40px, -70px) rotate(10deg) scale(0.88)",
  },
  {
    key: "company",
    label: "COMPANY CARD",
    icon: Share2,
    color: "#22d3ee",
    background: "linear-gradient(160deg, #042f36 0%, #0e7490 55%, #021b1f 100%)",
    borderColor: "rgba(34,211,238,0.35)",
    transform: "translate3d(16px, 24px, -42px) rotate(6deg) scale(0.93)",
  },
  {
    key: "review",
    label: "GOOGLE REVIEW",
    icon: Star,
    color: "#fbbf24",
    background: "linear-gradient(160deg, #2e2205 0%, #92640a 55%, #1a1303 100%)",
    borderColor: "rgba(251,191,36,0.4)",
    transform: "translate3d(7px, 10px, -18px) rotate(3deg) scale(0.97)",
  },
];

/**
 * The hero's 3D fanned NFC card stack: sways on its own, spins further as the hero
 * scrolls past, and tilts to follow the pointer. Rotation layers are split across three
 * nested elements (wrap -> carousel -> group) specifically so the pointer-tilt (group),
 * the auto/scroll rotation (carousel, driven every frame via rAF), and the idle keyframe
 * bob (group's own CSS animation) never overwrite each other's inline transform.
 */
export function HeroCardStack() {
  const wrapRef = useRef<HTMLDivElement>(null);
  const carouselRef = useRef<HTMLDivElement>(null);
  const groupRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const wrap = wrapRef.current;
    const group = groupRef.current;
    const carousel = carouselRef.current;
    let raf = 0;

    function handleMove(e: PointerEvent) {
      if (!wrap || !group) return;
      const rect = wrap.getBoundingClientRect();
      const px = (e.clientX - rect.left) / rect.width - 0.5;
      const py = (e.clientY - rect.top) / rect.height - 0.5;
      group.classList.add("manual-tilt");
      group.style.transform = `perspective(1000px) rotateX(${py * -16}deg) rotateY(${px * 20}deg) scale(1.02)`;
    }
    function handleLeave() {
      if (!group) return;
      group.classList.remove("manual-tilt");
      group.style.transform = "";
    }

    if (wrap && group && !reduceMotion) {
      wrap.addEventListener("pointermove", handleMove);
      wrap.addEventListener("pointerleave", handleLeave);
    }

    if (carousel && !reduceMotion) {
      const hero = document.getElementById("hero");
      const tick = (now: number) => {
        const t = now / 1000;
        const sway = Math.sin(t / 3.8) * 34;
        const tiltX = Math.cos(t / 3.1) * 7;
        let scrollBoost = 0;
        if (hero) {
          const rect = hero.getBoundingClientRect();
          const heroHeight = hero.offsetHeight || 1;
          const progress = Math.min(1, Math.max(0, -rect.top / heroHeight));
          scrollBoost = progress * 110;
        }
        carousel.style.transform = `rotateY(${sway + scrollBoost}deg) rotateX(${tiltX}deg)`;
        raf = requestAnimationFrame(tick);
      };
      raf = requestAnimationFrame(tick);
    }

    return () => {
      if (wrap) {
        wrap.removeEventListener("pointermove", handleMove);
        wrap.removeEventListener("pointerleave", handleLeave);
      }
      if (raf) cancelAnimationFrame(raf);
    };
  }, []);

  return (
    <div ref={wrapRef} className="relative flex items-center justify-center p-8" style={{ perspective: "1200px" }}>
      <div
        className="landing-ripple-ring absolute h-[190px] w-[300px] rounded-[26px] border"
        style={{ borderColor: "color-mix(in oklab, var(--primary) 55%, transparent)" }}
      />
      <div
        className="landing-ripple-ring delay absolute h-[190px] w-[300px] rounded-[26px] border"
        style={{ borderColor: "color-mix(in oklab, var(--primary) 55%, transparent)" }}
      />
      <div
        className="hero-float absolute bottom-1.5 h-10 w-[260px] rounded-full blur-[10px]"
        style={{
          background: "radial-gradient(ellipse, color-mix(in oklab, var(--primary) 55%, transparent), transparent 75%)",
        }}
      />

      <div ref={carouselRef} className="relative h-[202px] w-[320px]" style={{ transformStyle: "preserve-3d" }}>
        <div
          ref={groupRef}
          className="landing-idle-tilt relative h-full w-full"
          style={{ transformStyle: "preserve-3d", willChange: "transform" }}
        >
          {MINI_CARDS.map((mini) => (
            <div
              key={mini.key}
              className="absolute inset-0 flex flex-col items-center justify-center gap-2 rounded-[22px] border shadow-[0_20px_50px_-18px_rgba(0,0,0,0.6)]"
              style={{ transform: mini.transform, background: mini.background, borderColor: mini.borderColor, backfaceVisibility: "hidden" }}
            >
              <mini.icon className="size-5" style={{ color: mini.color }} />
              <span className="font-mono text-[10px] tracking-[0.12em]" style={{ color: "rgba(255,255,255,0.75)" }}>
                {mini.label}
              </span>
            </div>
          ))}

          <div
            className="absolute inset-0 overflow-hidden rounded-[22px] border shadow-[0_30px_80px_-20px_rgba(0,0,0,0.75),inset_0_1px_0_rgba(255,255,255,0.08)]"
            style={{
              background: "linear-gradient(155deg, #4338ca 0%, #4f46e5 45%, #1e1b4b 100%)",
              borderColor: "rgba(255,255,255,0.18)",
              backfaceVisibility: "hidden",
            }}
          >
            <div
              className="landing-sheen absolute -top-[60%] -left-[30%] h-[220%] w-[55%]"
              style={{
                background: "linear-gradient(100deg, transparent, rgba(255,255,255,0.22), transparent)",
                transform: "translateX(-40%) rotate(18deg)",
                mixBlendMode: "screen",
              }}
            />
            <div className="absolute inset-x-6 top-[22px] flex items-center justify-between" style={{ color: "rgba(255,255,255,0.85)" }}>
              <Nfc className="size-5" style={{ color: "var(--primary)" }} />
              <span className="text-sm font-semibold tracking-tight" style={{ color: "rgba(255,255,255,0.8)" }}>
                CeyloNfc
              </span>
            </div>
            <Nfc className="absolute -right-[18px] -bottom-[22px] size-[150px]" style={{ color: "rgba(255,255,255,0.08)" }} strokeWidth={1} />
            <div className="absolute inset-x-6 bottom-5 flex items-center justify-between">
              <Cpu className="size-[26px]" style={{ color: "#a3a3a3" }} />
              <span className="font-mono text-[10px] tracking-[0.14em]" style={{ color: "rgba(255,255,255,0.45)" }}>
                TAP TO CONNECT
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
