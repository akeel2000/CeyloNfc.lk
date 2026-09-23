"use client";

import { useState } from "react";
import { Box, Download, Globe, Mail, Phone, Square } from "lucide-react";

import { Dialog, DialogContent, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { TemplateLook2D } from "@/features/landing/template-look-2d";
import { TemplateLook3D } from "@/features/landing/template-look-3d";

const CONTACT_ROWS = [
  { icon: Phone, label: "Phone" },
  { icon: Mail, label: "Email" },
  { icon: Globe, label: "Website" },
];

/**
 * A template look, shown as a small thumbnail that opens into a fuller "real look" preview on
 * click - the same profile anatomy the actual public profile page renders (avatar, bio, social
 * links, contact rows, Save Contact button; see features/profile/public-profile-card.tsx),
 * themed by this look's accent color. Toggles between the "2D" and "3D" styles, matching the
 * same choice available in the profile editor (features/profile/phone-preview.tsx).
 */
export function TemplateLookCard({
  layout,
  name,
  color,
  personName,
  title,
  isIndividual = true,
}: {
  layout: string;
  name: string;
  color: string;
  personName?: string;
  title?: string;
  isIndividual?: boolean;
}) {
  const [mode, setMode] = useState<"2d" | "3d">("3d");
  const Preview = mode === "3d" ? TemplateLook3D : TemplateLook2D;

  return (
    <Dialog>
      <div className="glass-panel flex w-full flex-col gap-3 rounded-2xl p-4 text-center">
        <div className="flex justify-center gap-1 rounded-lg bg-white/[0.04] p-1" aria-label="Preview style">
          <button
            type="button"
            onClick={() => setMode("2d")}
            className={
              mode === "2d"
                ? "flex flex-1 items-center justify-center gap-1 rounded-md bg-white/10 py-1 text-[11px] font-medium text-white"
                : "flex flex-1 items-center justify-center gap-1 rounded-md py-1 text-[11px] text-white/50"
            }
          >
            <Square className="size-3" /> 2D
          </button>
          <button
            type="button"
            onClick={() => setMode("3d")}
            className={
              mode === "3d"
                ? "flex flex-1 items-center justify-center gap-1 rounded-md bg-white/10 py-1 text-[11px] font-medium text-white"
                : "flex flex-1 items-center justify-center gap-1 rounded-md py-1 text-[11px] text-white/50"
            }
          >
            <Box className="size-3" /> 3D
          </button>
        </div>

        <DialogTrigger asChild>
          <button type="button" className="landing-hover-lift">
            <Preview color={color} name={personName} title={title} isIndividual={isIndividual} compact />
          </button>
        </DialogTrigger>

        <div>
          <p className="text-sm font-semibold">{layout}</p>
          <p className="text-xs text-muted-foreground">{name}</p>
        </div>
      </div>

      <DialogContent size="sm" className="dark bg-background text-foreground">
        <DialogTitle>
          {layout} &mdash; {name}
        </DialogTitle>
        <div className="mx-auto w-full max-w-[260px]">
          <Preview color={color} name={personName} title={title} isIndividual={isIndividual} />
          <p className="mt-3 text-center text-xs leading-relaxed text-white/60">
            Helping businesses connect, one tap at a time.
          </p>

          <div className="mt-4 w-full space-y-2">
            {CONTACT_ROWS.map((row) => (
              <div key={row.label} className="glass-panel flex items-center gap-2.5 rounded-lg px-3 py-2 text-xs text-white/60">
                <row.icon className="size-3.5 shrink-0" />
                {row.label}
              </div>
            ))}
          </div>

          <button
            type="button"
            className="mt-4 flex w-full items-center justify-center gap-1.5 rounded-full py-2.5 text-xs font-semibold text-white"
            style={{ background: color }}
          >
            <Download className="size-3.5" />
            Save Contact
          </button>
        </div>
        <p className="text-center text-xs text-muted-foreground">
          Illustrative preview &mdash; not a real customer.
        </p>
      </DialogContent>
    </Dialog>
  );
}
