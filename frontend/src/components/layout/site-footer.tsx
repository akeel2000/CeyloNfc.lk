"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Nfc } from "lucide-react";

import { brand } from "@/lib/config/brand";
import { publicSettingsApi } from "@/lib/api/settings";

const FOOTER_LINKS = {
  Product: [
    { label: "Digital Business Card", href: "#products" },
    { label: "Google Review Card", href: "#products" },
    { label: "QR Solutions", href: "#products" },
  ],
  Company: [
    { label: "How It Works", href: "#how-it-works" },
    { label: "Contact", href: "#contact" },
  ],
  Legal: [
    { label: "Privacy Policy", href: "/privacy" },
    { label: "Terms of Service", href: "/terms" },
  ],
};

export function SiteFooter() {
  const { data: settings } = useQuery({
    queryKey: ["public", "settings"],
    queryFn: () => publicSettingsApi.get(),
    staleTime: 5 * 60 * 1000,
  });
  const siteName = settings?.siteName ?? brand.name;

  return (
    <footer className="border-t border-border bg-background">
      <div className="mx-auto max-w-6xl px-4 py-12 sm:px-6">
        <div className="grid gap-10 md:grid-cols-[2fr_1fr_1fr_1fr]">
          <div>
            <div className="flex items-center gap-2 font-semibold tracking-tight">
              <span className="flex size-8 items-center justify-center rounded-md bg-primary text-primary-foreground">
                <Nfc className="size-4" />
              </span>
              {siteName}
            </div>
            <p className="mt-3 max-w-xs text-sm text-muted-foreground">
              Smart NFC business cards, Google Review cards and digital profiles that connect
              your business instantly.
            </p>
            <div className="mt-4 space-y-1.5 text-sm text-muted-foreground">
              <p>
                <a href="tel:+94765441767" className="transition-colors hover:text-foreground">
                  076 544 1767
                </a>
              </p>
              <p>
                <a href="tel:+94752941767" className="transition-colors hover:text-foreground">
                  075 294 1767
                </a>
              </p>
              <p>
                <a href="mailto:noormohommaduakeel@gmail.com" className="transition-colors hover:text-foreground">
                  noormohommaduakeel@gmail.com
                </a>
              </p>
              <p>
                <a
                  href="https://ceylosoft.lk"
                  target="_blank"
                  rel="noreferrer"
                  className="transition-colors hover:text-foreground"
                >
                  Ceylosoft.lk
                </a>
              </p>
            </div>
          </div>

          {Object.entries(FOOTER_LINKS).map(([heading, links]) => (
            <div key={heading}>
              <h3 className="text-sm font-medium">{heading}</h3>
              <ul className="mt-3 space-y-2">
                {links.map((link) => (
                  <li key={link.label}>
                    <Link
                      href={link.href}
                      className="text-sm text-muted-foreground transition-colors hover:text-foreground"
                    >
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="mt-10 flex flex-col items-start justify-between gap-4 border-t border-border pt-6 text-xs text-muted-foreground sm:flex-row sm:items-center">
          <p>&copy; {new Date().getFullYear()} {siteName}. All rights reserved.</p>
        </div>
      </div>
    </footer>
  );
}
