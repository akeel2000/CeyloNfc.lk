import type { ReactNode } from "react";

import { SiteHeader } from "@/components/layout/site-header";
import { SiteFooter } from "@/components/layout/site-footer";
import { MONOCHROME_ACCENT } from "@/lib/config/theme";

export default function PublicLayout({ children }: { children: ReactNode }) {
  return (
    <div className="dark flex min-h-full flex-col bg-background text-foreground" style={MONOCHROME_ACCENT}>
      <SiteHeader />
      <main className="flex-1">{children}</main>
      <SiteFooter />
    </div>
  );
}
