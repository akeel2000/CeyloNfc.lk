import { z } from "zod";

export const profileUpdateSchema = z.object({
  slug: z
    .string()
    .min(1, "URL slug is required")
    .regex(/^[a-z0-9]+(-[a-z0-9]+)*$/, "Lowercase letters, numbers and hyphens only"),
  fullName: z.string().optional(),
  jobTitle: z.string().optional(),
  companyName: z.string().optional(),
  industry: z.string().optional(),
  registrationNumber: z.string().optional(),
  googleMapsUrl: z.string().optional(),
  logo: z.string().optional(),
  bio: z.string().max(1000).optional(),
  profileImage: z.string().optional(),
  coverImage: z.string().optional(),
  phone: z.string().optional(),
  whatsapp: z.string().optional(),
  email: z.string().email("Enter a valid email").optional().or(z.literal("")),
  website: z.string().optional(),
  address: z.string().optional(),
  city: z.string().optional(),
  country: z.string().optional(),
});

export type ProfileUpdateFormValues = z.infer<typeof profileUpdateSchema>;
