import { z } from "zod";

export const templateSchema = z.object({
  name: z.string().min(1, "Name is required").max(255),
  description: z.string().optional(),
  previewImage: z.string().optional(),
  primaryColor: z.string().regex(/^#[0-9A-Fa-f]{6}$/, "Enter a hex color like #4338ca"),
  layout: z.enum(["CLASSIC", "MINIMAL"]),
  premium: z.boolean(),
  active: z.boolean(),
  sortOrder: z.number({ error: "Enter a valid number" }).int(),
});

export type TemplateFormValues = z.infer<typeof templateSchema>;
