"use client";

import { useMemo, useState } from "react";
import { Box, Globe, Mail, MapPin, MessageCircle, Phone, Save, Sparkles } from "lucide-react";

import { Button } from "@/components/ui/button";
import { OrbitingSocialRing, type OrbitItem } from "@/features/profile/orbiting-social-ring";
import { SOCIAL_META, WEBSITE_META } from "@/features/profile/social-icons";
import type { ProfileUpdateFormValues } from "@/lib/schemas/profile";
import type { SocialLink } from "@/lib/types/profile";

const ACTIONS = [
  { icon: Phone, label: "Call" },
  { icon: MessageCircle, label: "WhatsApp" },
  { icon: Mail, label: "Email" },
  { icon: Save, label: "Save" },
];

export function PhonePreview({
  type,
  values,
  socialLinks,
}: {
  type: "INDIVIDUAL" | "BUSINESS";
  values: Partial<ProfileUpdateFormValues>;
  socialLinks: SocialLink[];
}) {
  const [mode, setMode] = useState<"2d" | "3d">("2d");
  const name = type === "INDIVIDUAL" ? values.fullName || "Your name" : values.companyName || "Your company";
  const subtitle = type === "INDIVIDUAL" ? values.jobTitle : values.industry;
  const avatarUrl = type === "INDIVIDUAL" ? values.profileImage : values.logo;
  const orbitItems = useMemo<OrbitItem[]>(() => {
    const socialItems = socialLinks
      .filter((link) => link.enabled && link.url)
      .sort((left, right) => left.displayOrder - right.displayOrder)
      .map((link) => ({ key: `${link.platform}-${link.url}`, href: link.url, ...SOCIAL_META[link.platform] }));

    return values.website
      ? [...socialItems, { key: "website", href: values.website, ...WEBSITE_META }]
      : socialItems;
  }, [socialLinks, values.website]);

  return (
    <div className={`mx-auto w-full ${mode === "2d" ? "max-w-[300px]" : "max-w-[320px]"}`}>
      <div className="mb-3 flex rounded-lg border bg-muted/50 p-1" aria-label="Preview style">
        <Button type="button" size="sm" variant={mode === "2d" ? "secondary" : "ghost"} className="flex-1" onClick={() => setMode("2d")}>
          2D profile
        </Button>
        <Button type="button" size="sm" variant={mode === "3d" ? "secondary" : "ghost"} className="flex-1" onClick={() => setMode("3d")}>
          <Box className="size-3.5" /> 3D style
        </Button>
      </div>

      {mode === "2d" ? (
        <TwoDimensionalPreview avatarUrl={avatarUrl} name={name} subtitle={subtitle} values={values} />
      ) : (
        <ThreeDimensionalPreview avatarUrl={avatarUrl} name={name} subtitle={subtitle} orbitItems={orbitItems} values={values} />
      )}
    </div>
  );
}

function TwoDimensionalPreview({ avatarUrl, name, subtitle, values }: PreviewProps) {
  return <div className="overflow-hidden rounded-[2rem] border-[6px] border-foreground bg-background shadow-xl">
    <div className="h-24 bg-gradient-to-br from-primary to-primary/60 bg-cover bg-center" style={values.coverImage ? { backgroundImage: `url(${values.coverImage})` } : undefined} />
    <div className="flex flex-col items-center px-4 pb-6 pt-0">
      <Avatar avatarUrl={avatarUrl} name={name} className="-mt-10 size-20 border-4 border-background text-lg" />
      <h3 className="mt-3 text-center text-lg font-semibold leading-tight">{name}</h3>
      {subtitle && <p className="text-center text-sm text-muted-foreground">{subtitle}</p>}
      <div className="mt-4 grid w-full grid-cols-4 gap-2">{ACTIONS.map((action) => <div key={action.label} className="flex flex-col items-center gap-1"><span className="flex size-10 items-center justify-center rounded-full bg-secondary"><action.icon className="size-4" /></span><span className="text-[10px] text-muted-foreground">{action.label}</span></div>)}</div>
      {values.bio && <p className="mt-4 text-center text-xs leading-relaxed text-muted-foreground">{values.bio}</p>}
      <ContactDetails values={values} />
    </div>
  </div>;
}

function ThreeDimensionalPreview({ avatarUrl, name, subtitle, orbitItems, values }: PreviewProps & { orbitItems: OrbitItem[] }) {
  return <div className="relative overflow-hidden rounded-[2rem] border-[6px] border-foreground bg-background px-3 py-6 text-foreground shadow-2xl">
    <div className="hero-particles absolute inset-0 opacity-20" />
    <div className="absolute -left-12 top-20 size-36 rounded-full bg-primary/20 blur-3xl" />
    <div className="absolute -right-12 bottom-12 size-40 rounded-full bg-chart-3/20 blur-3xl" />
    <div className="relative text-center"><span className="inline-flex items-center gap-1 rounded-full border border-primary/20 bg-primary/10 px-2.5 py-1 text-[10px] font-medium uppercase tracking-wider text-primary"><Sparkles className="size-3" /> Interactive 3D style</span></div>
    <OrbitingSocialRing items={orbitItems} compact>
      <div className="editor-3d-avatar relative flex size-32 items-center justify-center rounded-[2.2rem] bg-gradient-to-br from-primary via-accent-foreground to-chart-3 p-1 shadow-[0_25px_60px_-18px_rgba(201,162,39,0.9)]">
        <div className="absolute inset-3 rounded-[1.7rem] border border-white/25 bg-white/10" />
        <Avatar avatarUrl={avatarUrl} name={name} className="relative size-28 rounded-[1.7rem] border-2 border-white/70 text-xl shadow-lg" />
      </div>
    </OrbitingSocialRing>
    <div className="relative -mt-1 text-center"><h3 className="text-xl font-semibold">{name}</h3>{subtitle && <p className="mt-1 text-sm text-muted-foreground">{subtitle}</p>}<p className="mt-3 text-xs leading-relaxed text-muted-foreground">{values.bio || "Upload a profile photo to give your card a dimensional, interactive look."}</p></div>
    <div className="relative mt-5 grid grid-cols-4 gap-2">{ACTIONS.map((action) => <div key={action.label} className="flex flex-col items-center gap-1"><span className="flex size-9 items-center justify-center rounded-full border border-border bg-secondary"><action.icon className="size-3.5" /></span><span className="text-[9px] text-muted-foreground">{action.label}</span></div>)}</div>
    <div className="relative"><ContactDetails values={values} /></div>
  </div>;
}

type PreviewProps = { avatarUrl?: string | null; name: string; subtitle?: string; values: Partial<ProfileUpdateFormValues> };

function Avatar({ avatarUrl, name, className }: { avatarUrl?: string | null; name: string; className: string }) {
  return <div className={`flex items-center justify-center overflow-hidden rounded-full bg-secondary font-semibold text-foreground ${className}`}>{avatarUrl ? <img src={avatarUrl} alt="" className="size-full object-cover" /> : name.slice(0, 2).toUpperCase()}</div>;
}

function ContactDetails({ values }: { values: Partial<ProfileUpdateFormValues> }) {
  const items = [{ value: values.phone, icon: Phone }, { value: values.website, icon: Globe }, { value: values.address, icon: MapPin }].filter((item) => item.value);
  return items.length ? <div className="mt-4 w-full space-y-2 text-xs">{items.map(({ value, icon: Icon }) => <div key={value} className="flex items-center gap-2 rounded-md border border-border px-3 py-2"><Icon className="size-3.5 text-muted-foreground" /><span className="truncate">{value}</span></div>)}</div> : null;
}
