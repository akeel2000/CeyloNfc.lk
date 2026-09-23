import type { Metadata } from "next";
import { Gem, KeyRound, Nfc, Presentation, Star, Tag, Wand2 } from "lucide-react";

import { brand } from "@/lib/config/brand";
import { Reveal } from "@/features/landing/scroll-reveal";
import { CardMockup } from "@/features/landing/card-mockup";
import { SampleCardFlip } from "@/features/landing/sample-card-flip";
import { TemplateLookCard } from "@/features/landing/template-look-card";
import { QrImage, type QrStyle } from "@/features/qr/qr-image";

export const metadata: Metadata = {
  title: "Card Designs & Templates",
  description: `Every ${brand.name} card form factor, profile look and QR branding option in one place.`,
};

// Mirrors the real backend ProductType enum (backend/src/main/java/com/nfcplatform/product/entity/ProductType.java)
// and its admin-facing labels (frontend/src/features/products/product-dialog.tsx) - these are the actual
// physical SKU categories the platform sells, not invented card designs.
const CARD_TYPES = [
  { key: "BUSINESS_CARD", name: "Business card", icon: Nfc, description: "A slim NFC card sized for any wallet, ready to tap and share your profile.", background: "linear-gradient(155deg, #4338ca 0%, #4f46e5 55%, #1e1b4b 100%)" },
  { key: "METAL_CARD", name: "Metal card", icon: Gem, description: "A heavier, premium-feel metal card for a more tactile first impression.", background: "linear-gradient(155deg, #0369a1 0%, #0ea5e9 55%, #082f49 100%)" },
  { key: "GOOGLE_REVIEW_CARD", name: "Google review card", icon: Star, description: "Sends customers straight to your Google Review page in one tap.", background: "linear-gradient(155deg, #92400e 0%, #f59e0b 55%, #451a03 100%)" },
  { key: "REVIEW_STAND", name: "Review stand", icon: Presentation, description: "A countertop stand customers tap on their way out, right when a review is top of mind.", background: "linear-gradient(155deg, #0f766e 0%, #14b8a6 55%, #042f2e 100%)" },
  { key: "KEYCHAIN", name: "Keychain", icon: KeyRound, description: "A compact NFC keychain you carry everywhere, not just in a card slot.", background: "linear-gradient(155deg, #6d28d9 0%, #8b5cf6 55%, #2e1065 100%)" },
  { key: "TABLE_TAG", name: "Table tag", icon: Tag, description: "A small tag for tables, counters or shelves - tap to open a menu, page or profile.", background: "linear-gradient(155deg, #be123c 0%, #e11d48 55%, #4c0519 100%)" },
  { key: "CUSTOM", name: "Custom", icon: Wand2, description: "Tell us the destination and form factor - we'll configure a custom NFC solution.", background: "linear-gradient(155deg, #047857 0%, #10b981 55%, #022c22 100%)" },
];

// Illustrative accent-color examples, not a fixed catalog - Template.layout only has two real
// values (CLASSIC, MINIMAL; backend/src/main/java/com/nfcplatform/template/entity/TemplateLayout.java)
// and Template.primaryColor is a free hex field admins/clients set per template
// (backend/src/main/java/com/nfcplatform/template/entity/Template.java), so any color works.
// Templates themselves aren't scoped to individual vs. business (the same layout/color applies
// to either), but the two profile *types* render differently (person + circular avatar vs.
// company name + logo tile - see features/profile/public-profile-card.tsx), so these examples
// are split into both groups to show that difference, not to imply separate template catalogs.
const INDIVIDUAL_LOOKS = [
  { layout: "Classic", name: "Indigo", color: "#6366f1", personName: "Alex Perera", title: "Sales Manager" },
  { layout: "Classic", name: "Teal", color: "#0d9488", personName: "Nadeesha Fernando", title: "Photographer" },
  { layout: "Minimal", name: "Charcoal", color: "#71717a", personName: "Sanjay Perera", title: "Consultant" },
];

const BUSINESS_LOOKS = [
  { layout: "Classic", name: "Rose", color: "#e11d48", personName: "Ceylon Cafe", title: "Restaurant" },
  { layout: "Minimal", name: "Amber", color: "#d97706", personName: "Colombo Spice Co.", title: "Retail Store" },
];

const QR_EXAMPLES: { label: string; style: QrStyle }[] = [
  { label: "Classic black & white", style: { foreground: "#0a0a0a", background: "#ffffff", errorCorrectionLevel: "M" } },
  { label: "Inverted", style: { foreground: "#f5f5f5", background: "#171717", errorCorrectionLevel: "M" } },
  { label: "Brand accent", style: { foreground: "#4338ca", background: "#ffffff", errorCorrectionLevel: "M" } },
];

export default function CardDesignsPage() {
  return (
    <div className="mx-auto max-w-6xl px-4 py-20 sm:px-6">
      <Reveal className="mx-auto max-w-2xl text-center">
        <h1 className="text-3xl font-semibold tracking-tight sm:text-5xl">Card designs &amp; templates</h1>
        <p className="mt-4 text-muted-foreground">
          Every form factor we produce, every profile look you can apply, and how your QR code can
          carry your brand.
        </p>
      </Reveal>

      {/* Personalized sample */}
      <section className="mt-20">
        <Reveal className="mx-auto max-w-2xl text-center">
          <h2 className="text-2xl font-semibold tracking-tight">A real card, front and back</h2>
          <p className="mt-2 text-muted-foreground">
            Your logo and brand color on the front, a scannable QR fallback on the back.
          </p>
        </Reveal>
        <Reveal delay={80} className="mt-10">
          <SampleCardFlip />
        </Reveal>
      </section>

      {/* Card designs */}
      <section className="mt-24 border-t border-border pt-20">
        <Reveal>
          <h2 className="text-2xl font-semibold tracking-tight">Choose your card</h2>
          <p className="mt-2 text-muted-foreground">Seven form factors, one secure link architecture behind all of them.</p>
        </Reveal>
        <div className="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {CARD_TYPES.map((card, index) => (
            <Reveal key={card.key} delay={(index % 3) * 60}>
              <div className="landing-tilt-card glass-panel flex h-full flex-col gap-4 rounded-2xl p-5">
                <CardMockup icon={card.icon} label={card.name.toUpperCase()} background={card.background} />
                <div>
                  <h3 className="font-semibold tracking-tight">{card.name}</h3>
                  <p className="mt-1.5 text-sm leading-relaxed text-muted-foreground">{card.description}</p>
                </div>
              </div>
            </Reveal>
          ))}
        </div>
      </section>

      {/* Profile templates */}
      <section className="mt-24 border-t border-border pt-20">
        <Reveal>
          <h2 className="text-2xl font-semibold tracking-tight">Pick your profile look</h2>
          <p className="mt-2 max-w-2xl text-muted-foreground">
            Every profile uses one of two layouts - Classic or Minimal - and can be recolored to
            match your brand. Toggle 2D/3D on any card, or tap it to open the full look. These
            examples aren&apos;t a fixed set.
          </p>
        </Reveal>

        <div className="mt-10">
          <h3 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">For individuals</h3>
          <div className="mt-4 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {INDIVIDUAL_LOOKS.map((look, index) => (
              <Reveal key={look.personName} delay={(index % 3) * 50}>
                <TemplateLookCard
                  layout={look.layout}
                  name={look.name}
                  color={look.color}
                  personName={look.personName}
                  title={look.title}
                  isIndividual
                />
              </Reveal>
            ))}
          </div>
        </div>

        <div className="mt-12">
          <h3 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">For businesses</h3>
          <div className="mt-4 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {BUSINESS_LOOKS.map((look, index) => (
              <Reveal key={look.personName} delay={(index % 3) * 50}>
                <TemplateLookCard
                  layout={look.layout}
                  name={look.name}
                  color={look.color}
                  personName={look.personName}
                  title={look.title}
                  isIndividual={false}
                />
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      {/* QR branding */}
      <section className="mt-24 border-t border-border pt-20">
        <Reveal>
          <h2 className="text-2xl font-semibold tracking-tight">Brand your QR code</h2>
          <p className="mt-2 max-w-2xl text-muted-foreground">
            Every card ships with a matching QR code. Recolor it to match your brand, add your logo
            to the center, and export as PNG or SVG.
          </p>
        </Reveal>
        <div className="mt-10 grid gap-8 lg:grid-cols-[auto_1fr] lg:items-center">
          <div className="flex flex-wrap justify-center gap-6">
            {QR_EXAMPLES.map((example, index) => (
              <Reveal key={example.label} delay={index * 70} className="flex flex-col items-center gap-3">
                <div className="glass-panel rounded-2xl p-4">
                  <QrImage value={`${brand.domain}/t/sample`} size={140} style={example.style} />
                </div>
                <span className="text-xs text-muted-foreground">{example.label}</span>
              </Reveal>
            ))}
          </div>
          <Reveal delay={120}>
            <ul className="space-y-3 text-sm text-muted-foreground">
              <li>&bull; Custom foreground and background colors to match your brand.</li>
              <li>&bull; A logo embedded at the center (automatically switches to high error-correction so it still scans reliably).</li>
              <li>&bull; Four levels of error-correction, from fast-scanning to logo-safe.</li>
              <li>&bull; Export as PNG or SVG for print or web.</li>
            </ul>
          </Reveal>
        </div>
      </section>
    </div>
  );
}
