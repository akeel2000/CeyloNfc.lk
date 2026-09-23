import type { CSSProperties, ReactNode } from "react";

import type { SocialMeta } from "./social-icons";

export interface OrbitItem extends SocialMeta {
  key: string;
  href: string;
}

/**
 * Centers `children` (the person/company hero) and arranges `items` around it on a slowly
 * rotating orbit. Each icon's own transform is a rotate->translate->counter-rotate chain (see
 * globals.css `.orbit-icon` / `@keyframes orbit-spin`), so as the animation drives the outer
 * rotation from 0-360deg, the inner counter-rotation always exactly cancels it - the icon's
 * *position* travels around the circle while the glyph itself never visually rotates.
 */
export function OrbitingSocialRing({ items, children, compact = false }: { items: OrbitItem[]; children: ReactNode; compact?: boolean }) {
  const count = items.length;

  return (
    <div
      className="relative mx-auto flex items-center justify-center"
      style={
        {
          width: compact ? "clamp(220px, 62vw, 300px)" : "clamp(260px, 78vw, 380px)",
          height: compact ? "clamp(220px, 62vw, 300px)" : "clamp(260px, 78vw, 380px)",
        } as CSSProperties
      }
    >
      {children}

      {count > 0 && (
        <div
          className="pointer-events-none absolute inset-0"
          style={{ "--orbit-radius": compact ? "clamp(95px, 30vw, 135px)" : "clamp(112px, 37vw, 168px)" } as CSSProperties}
        >
          {items.map((item, index) => {
            const angle = (360 / count) * index;
            return (
              <div
                key={item.key}
                className="orbit-icon"
                style={
                  {
                    "--angle": `${angle}deg`,
                    "--radius": "var(--orbit-radius)",
                    "--orbit-duration": "36s",
                  } as CSSProperties
                }
              >
                <a
                  href={item.href}
                  target="_blank"
                  rel="noreferrer"
                  aria-label={item.label}
                  title={item.label}
                  className="orbit-icon-button pointer-events-auto"
                  style={{ "--glow": item.tint } as CSSProperties}
                >
                  <item.Icon className="size-[18px]" />
                </a>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
