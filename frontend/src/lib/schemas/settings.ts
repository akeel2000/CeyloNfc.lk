import { z } from "zod";

export const platformSettingsSchema = z.object({
  siteName: z.string().min(1, "Name is required").max(100),
  supportEmail: z.string().min(1, "Support email is required").email("Enter a valid email"),
  tagline: z.string().max(255).optional(),
});

export type PlatformSettingsFormValues = z.infer<typeof platformSettingsSchema>;
