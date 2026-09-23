import Link from "next/link";
import type { Metadata } from "next";
import {
  ArrowRight,
  BarChart3,
  ChevronDown,
  Nfc,
  QrCode,
  RefreshCcw,
  Share2,
  ShieldCheck,
  Smartphone,
  Star,
  UtensilsCrossed,
  Wand2,
  Zap,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { brand } from "@/lib/config/brand";
import { ContactLeadForm } from "@/features/leads/contact-lead-form";
import { Reveal } from "@/features/landing/scroll-reveal";
import { HeroCardStack } from "@/features/landing/hero-card-stack";
import { DemoPreview } from "@/features/landing/demo-preview";
import { FaqAccordion } from "@/features/landing/faq-accordion";
import { PromoBar } from "@/features/landing/promo-bar";
import { TapStoryIllustration } from "@/features/landing/tap-story-illustration";

export const metadata: Metadata = {
  title: { absolute: `${brand.name} - ${brand.tagline}` },
  description:
    "Modern NFC business cards, Google Review cards and digital profiles designed to connect your business instantly.",
};

const PRODUCTS = [
  {
    icon: Nfc,
    name: "Digital Business Card",
    description: "A tap-to-share profile with your contact details, links and a Save Contact button.",
  },
  {
    icon: Star,
    name: "Google Review NFC Card",
    description: "Send happy customers straight to your Google Review page in one tap.",
  },
  {
    icon: Share2,
    name: "Company NFC Card",
    description: "A branded profile for your business - team, services and contact channels in one place.",
  },
  {
    icon: UtensilsCrossed,
    name: "Smart Menu",
    description: "A mobile-first digital menu customers open instantly by tapping or scanning.",
  },
  {
    icon: QrCode,
    name: "QR Solutions",
    description: "Every card ships with a matching QR code for customers without NFC phones.",
  },
  {
    icon: Wand2,
    name: "Custom NFC Solutions",
    description: "Tell us the destination - link-in-bio, social, WhatsApp - we configure the rest.",
  },
];

const STEPS = [
  { step: "01", title: "Choose your NFC product", description: "Pick a card, stand or keychain that fits your business." },
  { step: "02", title: "We configure your card", description: "Each card gets a permanent, secure link registered to your account." },
  { step: "03", title: "Customize your profile", description: "Edit your digital profile any time - the physical card never changes." },
  { step: "04", title: "Tap, share and connect", description: "Customers tap or scan and land on your latest content instantly." },
];

const BENEFITS = [
  { icon: Smartphone, label: "No app required", description: "Customers just tap or scan with the phone they already have." },
  { icon: RefreshCcw, label: "Update details anytime", description: "Change your profile, links or menu whenever you need to - the card itself never changes." },
  { icon: Zap, label: "One tap connection", description: "No typing, no searching - your profile opens the instant they tap." },
  { icon: QrCode, label: "Built-in QR codes", description: "Every card ships with a matching QR code, so anyone without NFC can still connect." },
  { icon: ShieldCheck, label: "Secure NFC links", description: "Each card is registered to a permanent, secure link created just for your account." },
  { icon: BarChart3, label: "Tap & scan analytics", description: "See how many people tap or scan your card, and when." },
];

export default function LandingPage() {
  return (
    <div>
      <PromoBar />

      {/* Hero */}
      <section id="hero" className="relative overflow-hidden border-b border-border">
        <div
          className="pointer-events-none absolute -top-40 left-[8%] -z-10 size-[480px] rounded-full opacity-35 blur-[90px]"
          style={{ background: "var(--primary)" }}
        />
        <div
          className="pointer-events-none absolute -bottom-32 right-[4%] -z-10 size-[380px] rounded-full opacity-[0.18] blur-[90px]"
          style={{ background: "#a3a3a3" }}
        />
        <div className="hero-particles pointer-events-none absolute inset-0 -z-10 opacity-50" />

        <div className="mx-auto grid max-w-6xl items-center gap-10 px-4 pb-10 pt-20 sm:px-6 lg:grid-cols-[1fr_460px] lg:pt-24">
          <div className="text-center lg:text-left">
            <span className="glass-panel inline-flex items-center rounded-full px-3 py-1.5 text-xs font-medium text-foreground/75">
              Smart NFC for modern businesses
            </span>
            <h1 className="mt-6 text-4xl font-bold tracking-tight sm:text-6xl lg:text-[56px]">{brand.tagline}</h1>
            <p className="mx-auto mt-6 max-w-xl text-balance text-lg text-muted-foreground lg:mx-0">
              Modern NFC business cards, Google Review cards and digital profiles designed to
              connect your business instantly.
            </p>
            <div className="mt-8 flex flex-col justify-center gap-3 sm:flex-row lg:justify-start">
              <Button asChild size="lg" className="rounded-full">
                <a href="#contact">
                  Get Your NFC Card
                  <ArrowRight className="size-4" />
                </a>
              </Button>
              <Button asChild size="lg" variant="outline" className="rounded-full">
                <a href="#products">View Products</a>
              </Button>
            </div>
          </div>

          <HeroCardStack />
        </div>

        <a
          href="#products"
          className="mx-auto mb-10 flex w-fit flex-col items-center gap-1.5 text-[11px] font-mono tracking-[0.1em] text-foreground/40"
        >
          SCROLL
          <ChevronDown className="landing-scroll-bounce size-[18px]" />
        </a>
      </section>

      {/* Tap to share */}
      <section className="border-b border-border bg-white/[0.02] py-20">
        <div className="mx-auto max-w-6xl px-4 sm:px-6">
          <Reveal className="mx-auto max-w-2xl text-center">
            <TapStoryIllustration />
            <p className="mt-8 text-lg text-muted-foreground">From her card to his phone - instantly.</p>
          </Reveal>
        </div>
      </section>

      {/* Products */}
      <section id="products" className="mx-auto max-w-6xl px-4 py-24 sm:px-6">
        <Reveal className="max-w-2xl">
          <h2 className="text-3xl font-semibold tracking-tight">Built for every connection</h2>
          <p className="mt-3 text-muted-foreground">
            One platform, one secure link architecture, six ways to put it to work.
          </p>
          <Link href="/card-designs" className="mt-4 inline-block text-sm font-medium underline underline-offset-2">
            See every card design &rarr;
          </Link>
        </Reveal>
        <div className="mt-12 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {PRODUCTS.map((product, index) => (
            <Reveal key={product.name} delay={(index % 3) * 70}>
              <div className="landing-tilt-card glass-panel h-full rounded-2xl p-7">
                <span
                  className="mb-4 flex size-11 items-center justify-center rounded-xl"
                  style={{ background: "color-mix(in oklab, var(--primary) 18%, transparent)", color: "var(--primary)" }}
                >
                  <product.icon className="size-5" />
                </span>
                <h3 className="text-[17px] font-semibold tracking-tight">{product.name}</h3>
                <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{product.description}</p>
              </div>
            </Reveal>
          ))}
        </div>
      </section>

      {/* How it works */}
      <section id="how-it-works" className="border-y border-border bg-white/[0.02]">
        <div className="mx-auto max-w-6xl px-4 py-24 sm:px-6">
          <Reveal>
            <h2 className="text-3xl font-semibold tracking-tight">How it works</h2>
          </Reveal>
          <div className="relative mt-13 grid gap-8 sm:grid-cols-2 lg:grid-cols-4">
            <div className="pointer-events-none absolute inset-x-[6%] top-[17px] hidden h-px bg-gradient-to-r from-transparent via-border to-transparent lg:block" />
            {STEPS.map((item, index) => (
              <Reveal key={item.step} delay={index * 90}>
                <span
                  className="relative z-10 inline-flex size-[34px] items-center justify-center rounded-full border border-border bg-background font-mono text-xs"
                  style={{ color: "var(--primary)" }}
                >
                  {item.step}
                </span>
                <h3 className="mt-4 font-medium tracking-tight">{item.title}</h3>
                <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{item.description}</p>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      {/* Demo */}
      <section id="demo" className="mx-auto max-w-6xl px-4 py-24 text-center sm:px-6">
        <Reveal className="mx-auto max-w-2xl">
          <h2 className="text-3xl font-semibold tracking-tight">See it in action</h2>
          <p className="mt-3 text-muted-foreground">A single tap turns into a saved contact - no app, no typing.</p>
        </Reveal>
        <Reveal delay={80}>
          <DemoPreview />
        </Reveal>
      </section>

      {/* Benefits */}
      <section id="benefits" className="mx-auto max-w-6xl px-4 py-24 sm:px-6">
        <Reveal>
          <h2 className="text-3xl font-semibold tracking-tight">Why teams choose {brand.name}</h2>
        </Reveal>
        <div className="mt-12 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {BENEFITS.map((benefit, index) => (
            <Reveal key={benefit.label} delay={(index % 3) * 50}>
              <div className="landing-hover-lift glass-panel flex h-full items-start gap-3.5 rounded-2xl p-5">
                <span
                  className="flex size-[38px] shrink-0 items-center justify-center rounded-[10px] bg-white/[0.06]"
                  style={{ color: "var(--primary)" }}
                >
                  <benefit.icon className="size-4" />
                </span>
                <div>
                  <p className="text-sm font-medium">{benefit.label}</p>
                  <p className="mt-1 text-sm leading-relaxed text-muted-foreground">{benefit.description}</p>
                </div>
              </div>
            </Reveal>
          ))}
        </div>
      </section>

      {/* FAQ */}
      <section id="faq" className="border-y border-border bg-white/[0.02] py-24">
        <div className="mx-auto max-w-6xl px-4 text-center sm:px-6">
          <Reveal className="mx-auto max-w-2xl">
            <h2 className="text-3xl font-semibold tracking-tight">Frequently asked questions</h2>
            <p className="mt-3 text-muted-foreground">Everything else people usually ask before getting a card.</p>
          </Reveal>
          <FaqAccordion />
        </div>
      </section>

      {/* Contact */}
      <section id="contact" className="border-t border-border bg-primary py-24 text-center text-primary-foreground">
        <div className="mx-auto flex max-w-6xl flex-col items-center px-4 sm:px-6">
          <Reveal>
            <h2 className="text-3xl font-semibold tracking-tight">Ready to get your NFC card?</h2>
          </Reveal>
          <Reveal delay={60}>
            <p className="mt-3 max-w-xl text-primary-foreground/80">
              Reach out and we&apos;ll help you choose the right product and get your first card
              configured.
            </p>
          </Reveal>
          <Reveal delay={120} className="mt-8">
            <ContactLeadForm />
          </Reveal>
          <Reveal delay={160}>
            <div className="mt-4 flex flex-col gap-3 text-sm text-primary-foreground/70 sm:flex-row sm:items-center">
              <span>
                Prefer email?{" "}
                <a href={`mailto:${brand.supportEmail}`} className="underline underline-offset-2">
                  {brand.supportEmail}
                </a>
              </span>
              <span className="hidden sm:inline">&middot;</span>
              <Link href="/login" className="underline underline-offset-2">
                Client Login
              </Link>
            </div>
          </Reveal>
        </div>
      </section>
    </div>
  );
}
