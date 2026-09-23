import { z } from "zod";

export const reviewLocationSchema = z.object({
  businessName: z.string().min(1, "Business name is required"),
  locationName: z.string().optional(),
  address: z.string().optional(),
  googleMapsUrl: z.string().optional(),
  googleReviewUrl: z
    .string()
    .min(1, "Google Review link is required")
    .url("Enter a valid URL (include https://)"),
  googlePlaceId: z.string().optional(),
});

export type ReviewLocationFormValues = z.infer<typeof reviewLocationSchema>;
