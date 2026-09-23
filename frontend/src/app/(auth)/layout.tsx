import type { ReactNode } from "react";
import Link from "next/link";
import { Nfc } from "lucide-react";

import { brand } from "@/lib/config/brand";
import { MONOCHROME_ACCENT } from "@/lib/config/theme";

// Same monochrome accent as the public marketing site (see lib/config/theme.ts) - a visitor
// coming from the home page's "Client Login" link should land on a page that looks like the
// same site, not one that suddenly switches to the gold app brand.
export default function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div
      className="dark relative flex min-h-full flex-1 flex-col items-center justify-center overflow-hidden bg-background px-4 py-16 text-foreground"
      style={MONOCHROME_ACCENT}
    >
      <div className="hero-particles pointer-events-none absolute inset-0 -z-10 opacity-40" />
      <div className="pointer-events-none absolute -top-40 left-1/2 -z-10 size-[480px] -translate-x-1/2 rounded-full bg-white/[0.06] blur-[110px]" />

      <Link href="/" className="mb-8 flex items-center gap-2 font-semibold tracking-tight">
        <span className="flex size-8 items-center justify-center rounded-md bg-primary text-primary-foreground">
          <Nfc className="size-4" />
        </span>
        {brand.name}
      </Link>
      <div className="w-full max-w-sm">{children}</div>
    </div>
  );
}
