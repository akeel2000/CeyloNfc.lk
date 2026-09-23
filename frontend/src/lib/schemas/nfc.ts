import { z } from "zod";

export const nfcCardRegisterSchema = z.object({
  serialNumber: z.string().min(1, "Serial number is required").max(100),
  notes: z.string().optional(),
});

export type NfcCardRegisterFormValues = z.infer<typeof nfcCardRegisterSchema>;

export const nfcCardReplaceSchema = z.object({
  newSerialNumber: z.string().min(1, "Serial number is required").max(100),
  notes: z.string().optional(),
});

export type NfcCardReplaceFormValues = z.infer<typeof nfcCardReplaceSchema>;

const URL_BASED_TYPES = ["WEBSITE", "WHATSAPP", "SOCIAL", "CUSTOM_URL"] as const;

export const nfcCardAssignSchema = z
  .object({
    clientUuid: z.string().min(1, "Select a client"),
    destinationName: z.string().min(1, "Destination name is required"),
    destinationType: z.enum(["WEBSITE", "WHATSAPP", "SOCIAL", "CUSTOM_URL", "PROFILE", "COMPANY_PROFILE", "MENU", "GOOGLE_REVIEW", "VCARD"]),
    googleReviewLocationUuid: z.string().optional(),
    externalUrl: z.string().optional(),
  })
  .refine(
    (data) => {
      if (!URL_BASED_TYPES.includes(data.destinationType as (typeof URL_BASED_TYPES)[number])) return true;
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

export type NfcCardAssignFormValues = z.infer<typeof nfcCardAssignSchema>;
