"use client";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Nfc, Phone, Mail, Globe } from "lucide-react";
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
    <footer className="relative border-t border-blue-100 bg-slate-100">
      {" "}
      {/* Very Light Blue Top Border Accent */}{" "}
      <div className=" absolute inset-x-0 top-0 h-px bg-gradient-to-r from-blue-100 via-blue-200 to-blue-100 " />{" "}
      <div className="mx-auto max-w-6xl px-4 py-12 sm:px-6">
        {" "}
        <div className="grid items-start gap-10 md:grid-cols-[2fr_1fr_1fr_1fr]">
          {" "}
          {/* Brand & Contact */}{" "}
          <div className="min-w-0">
            {" "}
            <Link href="/" className="group inline-flex items-center gap-2">
              {" "}
              <span className=" relative flex size-8 shrink-0 items-center justify-center overflow-hidden rounded-md bg-slate-900 text-white shadow-md shadow-slate-900/10 transition-all duration-300 ease-out group-hover:-translate-y-0.5 group-hover:scale-105 group-hover:shadow-lg ">
                {" "}
                <span className=" absolute inset-0 bg-gradient-to-br from-blue-300/40 via-blue-400/10 to-transparent opacity-80 transition-opacity duration-300 group-hover:opacity-100 " />{" "}
                <Nfc className=" relative size-4 shrink-0 transition-transform duration-300 group-hover:scale-110 " />{" "}
              </span>{" "}
              <span className=" font-semibold tracking-tight text-slate-900 transition-colors duration-200 group-hover:text-blue-600 ">
                {" "}
                {siteName}{" "}
              </span>{" "}
            </Link>{" "}
            <p className="mt-3 max-w-xs text-sm leading-6 text-slate-500">
              {" "}
              Smart NFC business cards, Google Review cards and digital profiles
              that connect your business instantly.{" "}
            </p>{" "}
            {/* Contact Icons */}{" "}
            <div className="mt-5 flex items-center gap-2">
              {" "}
              {/* Phone */}{" "}
              <a
                href="tel:+94765441767"
                aria-label="Call 076 544 1767"
                className=" group flex size-9 shrink-0 items-center justify-center rounded-lg border border-blue-100 bg-white/70 text-slate-500 shadow-sm transition-all duration-200 ease-out hover:-translate-y-1 hover:border-slate-800 hover:bg-slate-800 hover:text-white hover:shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-300 "
              >
                {" "}
                <Phone className="block size-4 shrink-0 transition-transform duration-200 group-hover:scale-110" />{" "}
              </a>{" "}
              {/* Email */}{" "}
              <a
                href="mailto:noormohommaduakeel@gmail.com"
                aria-label="Send email"
                className=" group flex size-9 shrink-0 items-center justify-center rounded-lg border border-blue-100 bg-white/70 text-slate-500 shadow-sm transition-all duration-200 ease-out hover:-translate-y-1 hover:border-slate-800 hover:bg-slate-800 hover:text-white hover:shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-300 "
              >
                {" "}
                <Mail className="block size-4 shrink-0 transition-transform duration-200 group-hover:scale-110" />{" "}
              </a>{" "}
              {/* Website */}{" "}
              <a
                href="https://ceylosoft.lk/"
                target="_blank"
                rel="noreferrer"
                aria-label="Visit Ceylosoft website"
                className=" group flex size-9 shrink-0 items-center justify-center rounded-lg border border-blue-100 bg-white/70 text-slate-500 shadow-sm transition-all duration-200 ease-out hover:-translate-y-1 hover:border-slate-800 hover:bg-slate-800 hover:text-white hover:shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-300 "
              >
                {" "}
                <Globe className="block size-4 shrink-0 transition-transform duration-200 group-hover:scale-110" />{" "}
              </a>{" "}
            </div>{" "}
          </div>{" "}
          {/* Footer Link Groups */}{" "}
          {Object.entries(FOOTER_LINKS).map(([heading, links]) => (
            <div key={heading} className="min-w-0">
              {" "}
              <h3 className="text-sm font-semibold text-slate-900">
                {" "}
                {heading}{" "}
              </h3>{" "}
              <ul className="mt-4 space-y-2.5">
                {" "}
                {links.map((link) => (
                  <li key={link.label}>
                    {" "}
                    <Link
                      href={link.href}
                      className=" inline-flex rounded-sm text-sm leading-5 text-slate-500 transition-all duration-200 ease-out hover:translate-x-1 hover:text-slate-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-300 focus-visible:ring-offset-2 "
                    >
                      {" "}
                      {link.label}{" "}
                    </Link>{" "}
                  </li>
                ))}{" "}
              </ul>{" "}
            </div>
          ))}{" "}
        </div>{" "}
        {/* Copyright */}{" "}
        <div className=" mt-10 flex items-center justify-center border-t border-slate-200 pt-6 text-center text-xs text-slate-400 ">
          {" "}
          <p>
            {" "}
            &copy; {new Date().getFullYear()} {siteName}. All rights
            reserved.{" "}
          </p>{" "}
        </div>{" "}
      </div>{" "}
    </footer>
  );
}
