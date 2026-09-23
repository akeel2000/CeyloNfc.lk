import { z } from "zod";

export const qrCreateSchema = z
  .object({
    name: z.string().min(1, "Name is required"),
    destinationType: z.enum(["WEBSITE", "WHATSAPP", "SOCIAL", "CUSTOM_URL", "PROFILE", "COMPANY_PROFILE", "GOOGLE_REVIEW", "VCARD"]),
    externalUrl: z.string().optional(),
    googleReviewLocationUuid: z.string().optional(),
  })
  .refine(
    (data) => {
      if (!["WEBSITE", "WHATSAPP", "SOCIAL", "CUSTOM_URL"].includes(data.destinationType)) return true;
      return Boolean(data.externalUrl && /^https?:\/\/.+/.test(data.externalUrl));
    },
    { message: "Enter a valid URL (include https://)", path: ["externalUrl"] }
  )
  .refine(
    (data) => {
      if (data.destinationType !== "GOOGLE_REVIEW") return true;
      return Boolean(data.googleReviewLocationUuid);
    },
    { message: "Select a Google Review location", path: ["googleReviewLocationUuid"] }
  );

export type QrCreateFormValues = z.infer<typeof qrCreateSchema>;
