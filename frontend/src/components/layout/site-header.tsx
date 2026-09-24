"use client";
import { useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Menu, X, Nfc, ArrowUpRight } from "lucide-react";
import { brand } from "@/lib/config/brand";
import { publicSettingsApi } from "@/lib/api/settings";
const NAV_LINKS = [
  { label: "Home", href: "/" },
  { label: "Card Designs", href: "/card-designs" },
  { label: "Pricing", href: "/pricing" },
  { label: "Contact", href: "#contact" },
];
export function SiteHeader() {
  const [open, setOpen] = useState(false);
  const { data: settings } = useQuery({
    queryKey: ["public", "settings"],
    queryFn: () => publicSettingsApi.get(),
    staleTime: 5 * 60 * 1000,
  });
  const siteName = settings?.siteName ?? brand.name;
  return (
    <header className="sticky top-0 z-50 w-full border-b border-slate-200/60 bg-slate-50/80 px-3 py-3 backdrop-blur-xl sm:px-4 lg:px-6">
      <div className="mx-auto max-w-7xl">
        <div className="flex h-[68px] items-center justify-between gap-3 rounded-2xl border border-blue-200/50 bg-blue-50/70 px-3 shadow-lg shadow-blue-900/5 backdrop-blur-xl backdrop-saturate-150 transition-all duration-300 hover:shadow-xl hover:shadow-blue-900/10 sm:h-[72px] sm:px-4">
          <Link
            href="/"
            className="group flex min-w-0 items-center gap-2.5 sm:gap-3"
          >
            <div className="relative flex size-10 shrink-0 items-center justify-center overflow-hidden rounded-xl bg-slate-900 text-white shadow-md shadow-slate-900/10 transition-all duration-300 ease-out group-hover:-translate-y-0.5 group-hover:scale-105 group-hover:shadow-lg sm:size-11">
              <div className="absolute inset-0 bg-gradient-to-br from-blue-400/50 via-blue-500/10 to-transparent opacity-80 transition-opacity duration-300 group-hover:opacity-100" />
              <Nfc
                className="relative size-5 transition-transform duration-300 group-hover:scale-110 sm:size-6"
                strokeWidth={2}
              />
            </div>

            <div className="min-w-0 leading-none">
              <div className="truncate text-sm font-bold tracking-tight text-slate-900 transition-colors duration-200 group-hover:text-blue-700 sm:text-base lg:text-lg">
                {siteName}
              </div>
              <div className="mt-1 hidden text-[9px] font-medium uppercase tracking-[0.18em] text-slate-500 md:block">
                Digital Business Cards
              </div>
            </div>
          </Link>

          <nav className="hidden items-center gap-1 rounded-xl border border-blue-200/40 bg-white/30 p-1 backdrop-blur-md lg:flex">
            {NAV_LINKS.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className="rounded-lg px-3 py-2.5 text-sm font-medium text-slate-600 transition-all duration-200 ease-out hover:-translate-y-0.5 hover:bg-slate-800 hover:text-white hover:shadow-md active:translate-y-0 xl:px-4"
              >
                {link.label}
              </Link>
            ))}
          </nav>

          <div className="hidden items-center gap-2 lg:flex">
            <Link
              href="/login"
              className="inline-flex h-10 items-center justify-center rounded-xl border border-blue-200/50 bg-white/35 px-4 text-sm font-medium text-slate-700 backdrop-blur-md transition-all duration-200 ease-out hover:-translate-y-0.5 hover:border-slate-800 hover:bg-slate-800 hover:text-white hover:shadow-md active:translate-y-0"
            >
              Client Login
            </Link>

            <Link
              href="/#contact"
              className="group inline-flex h-10 items-center justify-center rounded-xl bg-slate-900 px-5 text-sm font-semibold text-white shadow-md shadow-slate-900/10 transition-all duration-200 ease-out hover:-translate-y-0.5 hover:bg-slate-800 hover:shadow-lg active:translate-y-0"
            >
              Get Your NFC Card
              <ArrowUpRight className="ml-1.5 size-4 transition-transform duration-300 group-hover:-translate-y-0.5 group-hover:translate-x-0.5" />
            </Link>
          </div>

          <button
            type="button"
            className="flex size-10 items-center justify-center rounded-xl border border-blue-200/50 bg-white/40 text-slate-800 shadow-sm backdrop-blur-md transition-all duration-200 ease-out hover:-translate-y-0.5 hover:bg-slate-800 hover:text-white hover:shadow-md active:translate-y-0 lg:hidden"
            aria-label={open ? "Close menu" : "Open menu"}
            aria-expanded={open}
            onClick={() => setOpen((value) => !value)}
          >
            {open ? (
              <X className="size-5 transition-transform duration-300" />
            ) : (
              <Menu className="size-5 transition-transform duration-300" />
            )}
          </button>
        </div>

        {open && (
          <div className="mt-2 overflow-hidden rounded-2xl border border-blue-200/50 bg-blue-50/80 shadow-xl shadow-blue-900/10 backdrop-blur-xl backdrop-saturate-150 animate-in fade-in slide-in-from-top-2 duration-200 lg:hidden">
            <nav className="p-3">
              {NAV_LINKS.map((link) => (
                <Link
                  key={link.href}
                  href={link.href}
                  onClick={() => setOpen(false)}
                  className="flex items-center rounded-xl px-4 py-3 text-sm font-medium text-slate-600 transition-all duration-200 ease-out hover:translate-x-1 hover:bg-slate-800 hover:text-white"
                >
                  {link.label}
                </Link>
              ))}

              <div className="mt-2 grid gap-2 border-t border-blue-200/50 pt-3">
                <Link
                  href="/login"
                  onClick={() => setOpen(false)}
                  className="inline-flex h-11 items-center justify-center rounded-xl border border-blue-200/50 bg-white/40 text-sm font-medium text-slate-700 shadow-sm backdrop-blur-md transition-all duration-200 ease-out hover:-translate-y-0.5 hover:border-slate-800 hover:bg-slate-800 hover:text-white hover:shadow-md active:translate-y-0"
                >
                  Client Login
                </Link>

                <Link
                  href="/#contact"
                  onClick={() => setOpen(false)}
                  className="group inline-flex h-11 items-center justify-center rounded-xl bg-slate-900 px-3 text-sm font-semibold text-white shadow-md transition-all duration-200 ease-out hover:-translate-y-0.5 hover:bg-slate-800 hover:shadow-lg active:translate-y-0"
                >
                  Get Your Card
                  <ArrowUpRight className="ml-1 size-4 transition-transform duration-300 group-hover:-translate-y-0.5 group-hover:translate-x-0.5" />
                </Link>
              </div>
            </nav>
          </div>
        )}
      </div>
    </header>
  );
}
