"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Menu, X, Nfc } from "lucide-react";

import { Button } from "@/components/ui/button";
import { brand } from "@/lib/config/brand";
import { publicSettingsApi } from "@/lib/api/settings";

const NAV_LINKS = [
  { label: "Home", href: "/" },
  { label: "Products", href: "#products" },
  { label: "How It Works", href: "#how-it-works" },
  { label: "Benefits", href: "#benefits" },
  { label: "Card Designs", href: "/card-designs" },
  { label: "Pricing", href: "/pricing" },
  { label: "Contact", href: "#contact" },
];

export function SiteHeader() {
  const [open, setOpen] = useState(false);
  // Falls back to the static brand.ts value if the backend is unreachable or the settings
  // haven't loaded yet - the header must never be blank while this query is in flight.
  const { data: settings } = useQuery({
    queryKey: ["public", "settings"],
    queryFn: () => publicSettingsApi.get(),
    staleTime: 5 * 60 * 1000,
  });
  const siteName = settings?.siteName ?? brand.name;

  return (
    <header className="sticky top-0 z-40 w-full border-b border-border bg-background/80 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6">
        <Link href="/" className="flex items-center gap-2 font-semibold tracking-tight">
          <span className="flex size-8 items-center justify-center rounded-md bg-primary text-primary-foreground">
            <Nfc className="size-4" />
          </span>
          {siteName}
        </Link>

        <nav className="hidden items-center gap-8 md:flex">
          {NAV_LINKS.map((link) =>
            link.href.startsWith("/") ? (
              <Link
                key={link.href}
                href={link.href}
                className="text-sm text-muted-foreground transition-colors hover:text-foreground"
              >
                {link.label}
              </Link>
            ) : (
              <a
                key={link.href}
                href={link.href}
                className="text-sm text-muted-foreground transition-colors hover:text-foreground"
              >
                {link.label}
              </a>
            )
          )}
        </nav>

        <div className="hidden items-center gap-3 md:flex">
          <Button asChild variant="ghost" size="sm">
            <Link href="/login">Client Login</Link>
          </Button>
          <Button asChild size="sm">
            <a href="#contact">Get Your NFC Card</a>
          </Button>
        </div>

        <button
          type="button"
          className="inline-flex items-center justify-center rounded-md p-2 text-foreground md:hidden"
          aria-label={open ? "Close menu" : "Open menu"}
          aria-expanded={open}
          onClick={() => setOpen((v) => !v)}
        >
          {open ? <X className="size-5" /> : <Menu className="size-5" />}
        </button>
      </div>

      {open && (
        <div className="border-t border-border bg-background md:hidden">
          <nav className="flex flex-col gap-1 px-4 py-4">
            {NAV_LINKS.map((link) =>
              link.href.startsWith("/") ? (
                <Link
                  key={link.href}
                  href={link.href}
                  onClick={() => setOpen(false)}
                  className="rounded-md px-2 py-2 text-sm text-muted-foreground hover:bg-accent hover:text-foreground"
                >
                  {link.label}
                </Link>
              ) : (
                <a
                  key={link.href}
                  href={link.href}
                  onClick={() => setOpen(false)}
                  className="rounded-md px-2 py-2 text-sm text-muted-foreground hover:bg-accent hover:text-foreground"
                >
                  {link.label}
                </a>
              )
            )}
            <div className="mt-2 flex flex-col gap-2 border-t border-border pt-4">
              <Button asChild variant="outline" size="sm">
                <Link href="/login">Client Login</Link>
              </Button>
              <Button asChild size="sm">
                <a href="#contact" onClick={() => setOpen(false)}>
                  Get Your NFC Card
                </a>
              </Button>
            </div>
          </nav>
        </div>
      )}
    </header>
  );
}
