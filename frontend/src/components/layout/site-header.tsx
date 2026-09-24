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
    <header className="sticky top-0 z-50 w-full bg-slate-50/80 px-4 py-4 sm:px-6">
      {" "}
      <div className="mx-auto max-w-7xl">
        {" "}
        {/* Main Glass Header */}{" "}
        <div className=" flex h-[72px] items-center justify-between rounded-2xl border border-blue-200/50 bg-blue-50/70 px-3 shadow-lg shadow-blue-900/5 backdrop-blur-xl backdrop-saturate-150 transition-all duration-300 hover:shadow-xl hover:shadow-blue-900/10 sm:px-5 ">
          {" "}
          {/* Brand */}{" "}
          <Link href="/" className="group flex items-center gap-3">
            {" "}
            <div className=" relative flex size-11 shrink-0 items-center justify-center overflow-hidden rounded-xl bg-slate-900 text-white shadow-md shadow-slate-900/10 transition-all duration-300 ease-out group-hover:-translate-y-0.5 group-hover:scale-105 group-hover:shadow-lg ">
              {" "}
              <div className=" absolute inset-0 bg-gradient-to-br from-blue-400/50 via-blue-500/10 to-transparent opacity-80 transition-opacity duration-300 group-hover:opacity-100 " />{" "}
              <Nfc
                className=" relative size-6 transition-transform duration-300 group-hover:scale-110 "
                strokeWidth={2}
              />{" "}
            </div>{" "}
            <div className="leading-none">
              {" "}
              <div className=" text-base font-bold tracking-tight text-slate-900 transition-colors duration-200 group-hover:text-blue-700 sm:text-lg ">
                {" "}
                {siteName}{" "}
              </div>{" "}
              <div className=" mt-1 hidden text-[9px] font-medium uppercase tracking-[0.18em] text-slate-500 sm:block ">
                {" "}
                Digital Business Cards{" "}
              </div>{" "}
            </div>{" "}
          </Link>{" "}
          {/* Desktop Navigation */}{" "}
          <nav className=" hidden items-center gap-1 rounded-xl border border-blue-200/40 bg-white/30 p-1 backdrop-blur-md md:flex ">
            {" "}
            {NAV_LINKS.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className=" rounded-lg px-4 py-2.5 text-sm font-medium text-slate-600 transition-all duration-200 ease-out hover:-translate-y-0.5 hover:bg-slate-800 hover:text-white hover:shadow-md active:translate-y-0 "
              >
                {" "}
                {link.label}{" "}
              </Link>
            ))}{" "}
          </nav>{" "}
          {/* Desktop Actions */}{" "}
          <div className="hidden items-center gap-2 md:flex">
            {" "}
            {/* Client Login */}{" "}
            <Link
              href="/login"
              className=" inline-flex h-10 items-center justify-center rounded-xl border border-blue-200/50 bg-white/35 px-4 text-sm font-medium text-slate-700 backdrop-blur-md transition-all duration-200 ease-out hover:-translate-y-0.5 hover:border-slate-800 hover:bg-slate-800 hover:text-white hover:shadow-md active:translate-y-0 "
            >
              {" "}
              Client Login{" "}
            </Link>{" "}
            {/* Get NFC Card */}{" "}
            <a
              href="#contact"
              className=" group inline-flex h-10 items-center justify-center rounded-xl bg-slate-900 px-5 text-sm font-semibold text-white shadow-md shadow-slate-900/10 transition-all duration-200 ease-out hover:-translate-y-0.5 hover:bg-slate-800 hover:shadow-lg active:translate-y-0 "
            >
              {" "}
              Get Your NFC Card{" "}
              <ArrowUpRight className=" ml-1.5 size-4 transition-transform duration-300 group-hover:-translate-y-0.5 group-hover:translate-x-0.5 " />{" "}
            </a>{" "}
          </div>{" "}
          {/* Mobile Menu Button */}{" "}
          <button
            type="button"
            className=" flex size-10 items-center justify-center rounded-xl border border-blue-200/50 bg-white/40 text-slate-800 shadow-sm backdrop-blur-md transition-all duration-200 ease-out hover:-translate-y-0.5 hover:bg-slate-800 hover:text-white hover:shadow-md active:translate-y-0 md:hidden "
            aria-label={open ? "Close menu" : "Open menu"}
            aria-expanded={open}
            onClick={() => setOpen((value) => !value)}
          >
            {" "}
            {open ? (
              <X className="size-5 transition-transform duration-300" />
            ) : (
              <Menu className="size-5 transition-transform duration-300" />
            )}{" "}
          </button>{" "}
        </div>{" "}
        {/* Mobile Glass Navigation */}{" "}
        {open && (
          <div className=" mt-2 overflow-hidden rounded-2xl border border-blue-200/50 bg-blue-50/80 shadow-xl shadow-blue-900/10 backdrop-blur-xl backdrop-saturate-150 animate-in fade-in slide-in-from-top-2 duration-200 md:hidden ">
            {" "}
            <nav className="p-3">
              {" "}
              {/* Mobile Links */}{" "}
              {NAV_LINKS.map((link) => (
                <Link
                  key={link.href}
                  href={link.href}
                  onClick={() => setOpen(false)}
                  className=" flex items-center rounded-xl px-4 py-3 text-sm font-medium text-slate-600 transition-all duration-200 ease-out hover:translate-x-1 hover:bg-slate-800 hover:text-white "
                >
                  {" "}
                  {link.label}{" "}
                </Link>
              ))}{" "}
              {/* Mobile Actions */}{" "}
              <div className=" mt-2 grid grid-cols-2 gap-2 border-t border-blue-200/50 pt-3 ">
                {" "}
                {/* Mobile Client Login */}{" "}
                <Link
                  href="/login"
                  onClick={() => setOpen(false)}
                  className=" inline-flex h-11 items-center justify-center rounded-xl border border-blue-200/50 bg-white/40 text-sm font-medium text-slate-700 shadow-sm backdrop-blur-md transition-all duration-200 ease-out hover:-translate-y-0.5 hover:border-slate-800 hover:bg-slate-800 hover:text-white hover:shadow-md active:translate-y-0 "
                >
                  {" "}
                  Client Login{" "}
                </Link>{" "}
                {/* Mobile Get NFC Card */}{" "}
                <a
                  href="#contact"
                  onClick={() => setOpen(false)}
                  className=" group inline-flex h-11 items-center justify-center rounded-xl bg-slate-900 px-3 text-sm font-semibold text-white shadow-md transition-all duration-200 ease-out hover:-translate-y-0.5 hover:bg-slate-800 hover:shadow-lg active:translate-y-0 "
                >
                  {" "}
                  Get Your Card{" "}
                  <ArrowUpRight className=" ml-1 size-4 transition-transform duration-300 group-hover:-translate-y-0.5 group-hover:translate-x-0.5 " />{" "}
                </a>{" "}
              </div>{" "}
            </nav>{" "}
          </div>
        )}{" "}
      </div>{" "}
    </header>
  );
}
