"use client";

import { useEffect, useRef, useState, type CSSProperties } from "react";
import {
  Building2,
  Download,
  Globe,
  Mail,
  MapPin,
  MessageCircle,
  Phone,
  QrCode,
  Share2,
} from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";
import { QrImage } from "@/features/qr/qr-image";
import { env } from "@/lib/config/env";
import { brand } from "@/lib/config/brand";
import { cn } from "@/lib/utils";
import type { PublicProfile } from "@/lib/types/profile";

import { OrbitingSocialRing, type OrbitItem } from "./orbiting-social-ring";
import { SOCIAL_META, WEBSITE_META } from "./social-icons";

// Matches --primary in dark mode (globals.css) so an unbranded profile still reads as this
// product's gold, not an unrelated color.
const DEFAULT_ACCENT = "#c9a227";

const DAY_LABELS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

interface QuickAction {
  key: string;
  label: string;
  href: string;
  Icon: typeof Phone;
}

export function PublicProfileCard({ profile, vcardPath }: { profile: PublicProfile; vcardPath: string }) {
  const isIndividual = profile.type === "INDIVIDUAL";
  const isMinimal = profile.templateLayout === "MINIMAL";
  const name = isIndividual ? profile.fullName : profile.companyName;
  const subtitle = isIndividual ? profile.jobTitle : profile.industry;
  const avatarUrl = isIndividual ? profile.profileImage : profile.logo;
  const accent = profile.templatePrimaryColor || DEFAULT_ACCENT;
  const locationLine = [profile.city, profile.country].filter(Boolean).join(", ");
  const openBadge = !isIndividual && profile.openStatusLabel && (
    <span
      className={cn(
        "mt-2 inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-medium",
        profile.openNow
          ? "border-success/30 bg-success/10 text-success"
          : "border-white/15 bg-white/5 text-white/60"
      )}
    >
      <span className={cn("size-1.5 rounded-full", profile.openNow ? "bg-success" : "bg-white/40")} />
      {profile.openStatusLabel}
    </span>
  );
  const vcardUrl = `${env.apiUrl}${vcardPath}`;

  const [qrOpen, setQrOpen] = useState(false);
  const [heroProgress, setHeroProgress] = useState(0);
  const heroRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (reduceMotion) return;

    let ticking = false;
    const onScroll = () => {
      if (ticking) return;
      ticking = true;
      requestAnimationFrame(() => {
        const heroHeight = heroRef.current?.offsetHeight || 1;
        setHeroProgress(Math.min(1, Math.max(0, window.scrollY / (heroHeight * 0.7))));
        ticking = false;
      });
    };
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  const orbitItems: OrbitItem[] = [];
  for (const link of profile.socialLinks) {
    const meta = SOCIAL_META[link.platform];
    if (!link.url || !meta) continue;
    orbitItems.push({ key: `${link.platform}-${link.url}`, href: link.url, ...meta });
  }
  if (profile.website) {
    orbitItems.push({ key: "website-orbit", href: profile.website, ...WEBSITE_META });
  }

  const quickActions: QuickAction[] = [
    profile.phone && { key: "call", label: "Call", href: `tel:${profile.phone}`, Icon: Phone },
    profile.whatsapp && {
      key: "whatsapp",
      label: "WhatsApp",
      href: `https://wa.me/${profile.whatsapp.replace(/[^0-9]/g, "")}`,
      Icon: MessageCircle,
    },
    profile.email && { key: "email", label: "Email", href: `mailto:${profile.email}`, Icon: Mail },
    profile.website && { key: "website", label: "Website", href: profile.website, Icon: Globe },
  ].filter(Boolean) as QuickAction[];

  const bottomBarActions = quickActions.filter((a) => a.key === "call" || a.key === "whatsapp");
  const hasConnectSection = orbitItems.length > 0;
  const hasBusinessHours = Boolean(!isIndividual && profile.businessHours && profile.businessHours.length > 0);
  const hasContactSection = Boolean(
    profile.phone || profile.whatsapp || profile.email || profile.website || profile.address || locationLine || hasBusinessHours
  );

  async function handleShare() {
    const url = window.location.href;
    if (typeof navigator !== "undefined" && navigator.share) {
      try {
        await navigator.share({ title: name ?? brand.name, url });
      } catch {
        // user cancelled the share sheet - not an error
      }
      return;
    }
    try {
      await navigator.clipboard.writeText(url);
      toast.success("Profile link copied");
    } catch {
      toast.error("Couldn't copy the link");
    }
  }

  function scrollToConnect() {
    const behavior = window.matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth";
    document.getElementById("connect")?.scrollIntoView({ behavior, block: "start" });
  }

  const heroStyle = {
    transform: `translateY(${heroProgress * -28}px) scale(${1 - heroProgress * 0.08})`,
    opacity: 1 - heroProgress * 0.55,
  };

  return (
    <div className="dark min-h-full bg-background text-foreground" style={{ "--primary": accent } as CSSProperties}>
      <div className="mx-auto flex min-h-full max-w-md flex-col pb-28 md:max-w-lg md:pb-16">
        {/* Hero */}
        <div ref={heroRef} className="relative flex flex-col items-center overflow-hidden">
          {isMinimal ? (
            <>
              {/* Top bar */}
              <div className="relative z-20 flex w-full items-center justify-between px-4 pt-4">
                <span className="text-sm font-semibold tracking-wide text-white/80">{brand.name}</span>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setQrOpen(true)}
                    aria-label="Show QR code"
                    className="flex size-9 items-center justify-center rounded-full border border-white/15 bg-white/5 text-white/80 backdrop-blur-md transition-colors hover:bg-white/10"
                  >
                    <QrCode className="size-4" />
                  </button>
                  <button
                    type="button"
                    onClick={handleShare}
                    aria-label="Share profile"
                    className="flex size-9 items-center justify-center rounded-full border border-white/15 bg-white/5 text-white/80 backdrop-blur-md transition-colors hover:bg-white/10"
                  >
                    <Share2 className="size-4" />
                  </button>
                </div>
              </div>

              <div className="relative flex w-full flex-col items-center px-6 pb-8 pt-6">
                <div className="pointer-events-none absolute inset-0 -z-10 bg-background">
                  <div
                    className="absolute -top-16 left-1/2 h-72 w-72 -translate-x-1/2 rounded-full opacity-40 blur-[90px]"
                    style={{ background: accent }}
                  />
                  <div
                    className="absolute bottom-0 right-0 h-56 w-56 rounded-full opacity-25 blur-[80px]"
                    style={{ background: "var(--info)" }}
                  />
                </div>

                <div style={heroStyle}>
                  <OrbitingSocialRing items={orbitItems} compact>
                    <div className="hero-float relative">
                      <div
                        className="absolute inset-[-20%] rounded-full opacity-60 blur-2xl"
                        style={{ background: `radial-gradient(circle, ${accent}66, transparent 70%)` }}
                      />
                      <div
                        className={cn(
                          "relative flex items-center justify-center overflow-hidden border border-white/15 bg-white/5 text-3xl font-semibold text-white/80 shadow-[0_20px_60px_-15px_rgba(0,0,0,0.7)] backdrop-blur-sm",
                          isIndividual ? "size-32 rounded-full" : "size-32 rounded-[1.75rem]"
                        )}
                      >
                        {avatarUrl ? (
                          // eslint-disable-next-line @next/next/no-img-element -- external, unpredictable-origin uploaded images
                          <img src={avatarUrl} alt={name ?? ""} className="size-full object-cover" />
                        ) : isIndividual ? (
                          (name ?? "?").slice(0, 2).toUpperCase()
                        ) : (
                          <Building2 className="size-10 text-white/50" />
                        )}
                      </div>
                      <div
                        className="absolute left-1/2 top-full h-3 w-24 -translate-x-1/2 -translate-y-1 rounded-full opacity-70 blur-md"
                        style={{ background: `radial-gradient(ellipse, ${accent}aa, transparent 75%)` }}
                      />
                    </div>
                  </OrbitingSocialRing>
                </div>

                <h1 className="mt-6 text-center text-2xl font-semibold tracking-tight text-white">{name}</h1>
                {subtitle && <p className="mt-1 text-center text-white/60">{subtitle}</p>}
                {isIndividual && profile.companyName && (
                  <p className="text-sm text-white/45">{profile.companyName}</p>
                )}
                {locationLine && (
                  <p className="mt-1 flex items-center gap-1 text-sm text-white/45">
                    <MapPin className="size-3.5" />
                    {locationLine}
                  </p>
                )}
                {openBadge}
                {profile.bio && (
                  <p className="mt-4 max-w-xs text-center text-sm leading-relaxed text-white/70">{profile.bio}</p>
                )}
              </div>
            </>
          ) : (
            <>
              {/* Cover banner - profile.coverImage, falling back to a brand-accent gradient */}
              <div className="relative h-40 w-full shrink-0 overflow-hidden bg-black/40 sm:h-48" style={heroStyle}>
                {profile.coverImage ? (
                  // eslint-disable-next-line @next/next/no-img-element -- external, unpredictable-origin uploaded images
                  <img src={profile.coverImage} alt="" className="size-full object-cover" />
                ) : (
                  <div
                    className="relative size-full"
                    style={{ background: `linear-gradient(135deg, ${accent}55, transparent 70%)` }}
                  >
                    <div className="hero-particles absolute inset-0 opacity-50" />
                  </div>
                )}
                <div className="absolute inset-0 bg-gradient-to-t from-background via-transparent to-black/30" />
                <div className="absolute inset-0 bg-gradient-to-b from-black/40 via-transparent to-transparent" />
              </div>

              {/* Top bar, overlaid on the cover banner */}
              <div className="absolute inset-x-0 top-0 z-20 flex items-center justify-between px-4 pt-4">
                <span className="text-sm font-semibold tracking-wide text-white/90 drop-shadow">{brand.name}</span>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setQrOpen(true)}
                    aria-label="Show QR code"
                    className="flex size-9 items-center justify-center rounded-full border border-white/15 bg-white/10 text-white/90 backdrop-blur-md transition-colors hover:bg-white/20"
                  >
                    <QrCode className="size-4" />
                  </button>
                  <button
                    type="button"
                    onClick={handleShare}
                    aria-label="Share profile"
                    className="flex size-9 items-center justify-center rounded-full border border-white/15 bg-white/10 text-white/90 backdrop-blur-md transition-colors hover:bg-white/20"
                  >
                    <Share2 className="size-4" />
                  </button>
                </div>
              </div>

              {/* Avatar overlaps the bottom edge of the banner, LinkedIn-style */}
              <div className="relative z-10 -mt-14 flex w-full flex-col items-center px-6 pb-10 sm:-mt-16">
                <div className="hero-float relative">
                  {/* Soft conic gradient ring behind the avatar - an Instagram "story ring" nod, kept subtle/premium */}
                  <div
                    className="rounded-full p-[3px]"
                    style={{ background: `conic-gradient(from 180deg, ${accent}, var(--chart-3), ${accent})` }}
                  >
                    <div
                      className={cn(
                        "relative flex items-center justify-center overflow-hidden border-4 border-background bg-white/5 text-3xl font-semibold text-white/80 shadow-[0_20px_60px_-15px_rgba(0,0,0,0.7)]",
                        isIndividual ? "size-28 rounded-full" : "size-28 rounded-[1.5rem]"
                      )}
                    >
                      {avatarUrl ? (
                        // eslint-disable-next-line @next/next/no-img-element -- external, unpredictable-origin uploaded images
                        <img src={avatarUrl} alt={name ?? ""} className="size-full object-cover" />
                      ) : isIndividual ? (
                        (name ?? "?").slice(0, 2).toUpperCase()
                      ) : (
                        <Building2 className="size-9 text-white/50" />
                      )}
                    </div>
                  </div>
                </div>

                <h1 className="mt-4 text-center text-2xl font-semibold tracking-tight text-white">{name}</h1>
                {subtitle && <p className="mt-1 text-center text-white/60">{subtitle}</p>}
                {isIndividual && profile.companyName && (
                  <p className="text-sm text-white/45">{profile.companyName}</p>
                )}
                {locationLine && (
                  <p className="mt-1 flex items-center gap-1 text-sm text-white/45">
                    <MapPin className="size-3.5" />
                    {locationLine}
                  </p>
                )}
                {openBadge}
                {profile.bio && (
                  <p className="mt-4 max-w-xs text-center text-sm leading-relaxed text-white/70">{profile.bio}</p>
                )}
              </div>
            </>
          )}

          <div className="relative z-10 flex w-full flex-col items-center px-6 pb-2">
            <div className="flex w-full max-w-xs gap-3">
            <Button asChild size="lg" className="flex-1 shadow-lg shadow-primary/30">
              <a href={vcardUrl}>
                <Download className="size-4" />
                Save Contact
              </a>
            </Button>
            {hasConnectSection && (
              <Button
                type="button"
                variant="outline"
                size="lg"
                onClick={scrollToConnect}
                className="flex-1 border-white/15 bg-white/5 text-white hover:bg-white/10"
              >
                Connect
              </Button>
            )}
          </div>

          {quickActions.length > 0 && (
            <div className="mt-5 grid w-full max-w-xs grid-cols-4 gap-3">
              {quickActions.map((action) => (
                <a
                  key={action.key}
                  href={action.href}
                  target={action.key === "website" || action.key === "whatsapp" ? "_blank" : undefined}
                  rel={action.key === "website" || action.key === "whatsapp" ? "noreferrer" : undefined}
                  className="flex flex-col items-center gap-1.5"
                >
                  <span className="flex size-11 items-center justify-center rounded-full border border-white/15 bg-white/5 text-white/80 backdrop-blur-sm transition-colors hover:bg-white/10">
                    <action.Icon className="size-4" />
                  </span>
                  <span className="text-[11px] text-white/50">{action.label}</span>
                </a>
              ))}
            </div>
          )}
          </div>
        </div>

        {/* Content below hero */}
        <div className="flex flex-1 flex-col gap-6 px-4 pt-2">
          {profile.bio && (
            <section className="glass-panel rounded-2xl p-5">
              <h2 className="text-sm font-semibold uppercase tracking-wide text-white/50">About Me</h2>
              <p className="mt-3 text-sm leading-relaxed text-white/75">{profile.bio}</p>
            </section>
          )}

          {hasConnectSection && (
            <section id="connect" className="scroll-mt-6">
              <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-white/50">Connect With Me</h2>
              <div className="grid grid-cols-2 gap-3">
                {orbitItems.map((item) => (
                  <a
                    key={item.key}
                    href={item.href}
                    target="_blank"
                    rel="noreferrer"
                    className="glass-panel flex items-center gap-3 rounded-xl px-4 py-3 transition-colors hover:bg-white/[0.08]"
                  >
                    <span
                      className="flex size-9 shrink-0 items-center justify-center rounded-full"
                      style={{ background: `${item.tint}26`, color: item.tint }}
                    >
                      <item.Icon className="size-4" />
                    </span>
                    <span className="min-w-0">
                      <span className="block text-sm font-medium text-white/85">{item.label}</span>
                      <span className="block truncate text-xs text-white/40">
                        {item.href.replace(/^https?:\/\//, "")}
                      </span>
                    </span>
                  </a>
                ))}
              </div>
            </section>
          )}

          {hasContactSection && (
            <section id="contact" className="scroll-mt-6">
              <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-white/50">Contact</h2>
              <div className="space-y-2">
                {profile.phone && (
                  <a href={`tel:${profile.phone}`} className="glass-panel flex items-center gap-3 rounded-xl px-4 py-3 text-sm text-white/80 hover:bg-white/[0.08]">
                    <Phone className="size-4 shrink-0 text-white/45" />
                    {profile.phone}
                  </a>
                )}
                {profile.whatsapp && (
                  <a
                    href={`https://wa.me/${profile.whatsapp.replace(/[^0-9]/g, "")}`}
                    target="_blank"
                    rel="noreferrer"
                    className="glass-panel flex items-center gap-3 rounded-xl px-4 py-3 text-sm text-white/80 hover:bg-white/[0.08]"
                  >
                    <MessageCircle className="size-4 shrink-0 text-white/45" />
                    {profile.whatsapp}
                  </a>
                )}
                {profile.email && (
                  <a href={`mailto:${profile.email}`} className="glass-panel flex items-center gap-3 rounded-xl px-4 py-3 text-sm text-white/80 hover:bg-white/[0.08]">
                    <Mail className="size-4 shrink-0 text-white/45" />
                    {profile.email}
                  </a>
                )}
                {profile.website && (
                  <a
                    href={profile.website}
                    target="_blank"
                    rel="noreferrer"
                    className="glass-panel flex items-center gap-3 rounded-xl px-4 py-3 text-sm text-white/80 hover:bg-white/[0.08]"
                  >
                    <Globe className="size-4 shrink-0 text-white/45" />
                    <span className="truncate">{profile.website.replace(/^https?:\/\//, "")}</span>
                  </a>
                )}
                {profile.address && (
                  <a
                    href={profile.googleMapsUrl || `https://maps.google.com/?q=${encodeURIComponent(profile.address)}`}
                    target="_blank"
                    rel="noreferrer"
                    className="glass-panel flex items-center gap-3 rounded-xl px-4 py-3 text-sm text-white/80 hover:bg-white/[0.08]"
                  >
                    <MapPin className="size-4 shrink-0 text-white/45" />
                    <span>
                      {profile.address}
                      {profile.city && `, ${profile.city}`}
                      {profile.country && `, ${profile.country}`}
                    </span>
                  </a>
                )}
              </div>

              {!isIndividual && profile.businessHours && profile.businessHours.length > 0 && (
                <div className="glass-panel mt-3 rounded-xl px-4 py-3">
                  {profile.openStatusLabel && (
                    <p className={cn("mb-2 text-sm font-medium", profile.openNow ? "text-success" : "text-white/60")}>
                      {profile.openStatusLabel}
                    </p>
                  )}
                  <dl className="space-y-1 text-xs text-white/60">
                    {DAY_LABELS.map((label, i) => {
                      const dayOfWeek = i + 1;
                      const hour = profile.businessHours!.find((h) => h.dayOfWeek === dayOfWeek);
                      const isToday = new Date().getDay() === dayOfWeek % 7;
                      return (
                        <div key={dayOfWeek} className={cn("flex justify-between", isToday && "font-medium text-white/85")}>
                          <dt>{label}</dt>
                          <dd>{hour && !hour.closed ? `${hour.opensAt} - ${hour.closesAt}` : "Closed"}</dd>
                        </div>
                      );
                    })}
                  </dl>
                </div>
              )}

              <div className="mt-4 flex gap-3">
                <Button asChild size="lg" className="flex-1">
                  <a href={vcardUrl}>
                    <Download className="size-4" />
                    Save Contact
                  </a>
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  size="lg"
                  onClick={handleShare}
                  className="flex-1 border-white/15 bg-white/5 text-white hover:bg-white/10"
                >
                  <Share2 className="size-4" />
                  Share Profile
                </Button>
              </div>
            </section>
          )}

          <p className="mb-2 mt-4 text-center text-xs text-white/30">Powered by {brand.name}</p>
        </div>
      </div>

      {/* Mobile floating action bar */}
      {(bottomBarActions.length > 0) && (
        <div
          className="fixed inset-x-0 bottom-0 z-30 flex justify-center px-4 md:hidden"
          style={{ paddingBottom: "max(env(safe-area-inset-bottom), 14px)" }}
        >
          <div className="glass-panel flex w-full max-w-md items-center justify-around rounded-2xl bg-background/90 px-2 py-2 shadow-2xl">
            {bottomBarActions.map((action) => (
              <a
                key={action.key}
                href={action.href}
                target={action.key === "whatsapp" ? "_blank" : undefined}
                rel={action.key === "whatsapp" ? "noreferrer" : undefined}
                className="flex flex-1 flex-col items-center gap-1 rounded-xl py-1.5 text-white/70 transition-colors hover:bg-white/5 hover:text-white"
              >
                <action.Icon className="size-4" />
                <span className="text-[10px]">{action.label}</span>
              </a>
            ))}
            <a href={vcardUrl} className="flex flex-1 flex-col items-center gap-1 rounded-xl py-1.5 text-white/70 transition-colors hover:bg-white/5 hover:text-white">
              <Download className="size-4" />
              <span className="text-[10px]">Save</span>
            </a>
            <button
              type="button"
              onClick={handleShare}
              className="flex flex-1 flex-col items-center gap-1 rounded-xl py-1.5 text-white/70 transition-colors hover:bg-white/5 hover:text-white"
            >
              <Share2 className="size-4" />
              <span className="text-[10px]">Share</span>
            </button>
          </div>
        </div>
      )}

      <Dialog open={qrOpen} onOpenChange={setQrOpen}>
        <DialogContent size="sm">
          <DialogTitle className="text-center text-base">Scan to open this profile</DialogTitle>
          <div className="flex justify-center py-2">
            {qrOpen && typeof window !== "undefined" && <QrImage value={window.location.href} size={220} />}
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
