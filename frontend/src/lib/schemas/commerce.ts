import { z } from "zod";

const optionalLimit = z
  .union([z.number({ error: "Enter a valid number" }).int().min(0), z.nan()])
  .optional();

export function toLimitOrNull(value: number | undefined): number | null {
  return value === undefined || Number.isNaN(value) ? null : value;
}

export const packagePlanSchema = z.object({
  name: z.string().min(1, "Name is required").max(255),
  description: z.string().optional(),
  price: z.number({ error: "Enter a valid price" }).min(0, "Price must be 0 or more"),
  billingPeriod: z.enum(["MONTHLY", "YEARLY", "ONE_TIME"]),
  cardLimit: optionalLimit,
  profileLimit: optionalLimit,
  reviewLocationLimit: optionalLimit,
  menuLimit: optionalLimit,
  premiumTemplates: z.boolean(),
  active: z.boolean(),
  sortOrder: z.number({ error: "Enter a valid number" }).int(),
});

export type PackagePlanFormValues = z.infer<typeof packagePlanSchema>;

export interface PackagePlanPayload
  extends Omit<PackagePlanFormValues, "cardLimit" | "profileLimit" | "reviewLocationLimit" | "menuLimit"> {
  cardLimit: number | null;
  profileLimit: number | null;
  reviewLocationLimit: number | null;
  menuLimit: number | null;
}

export function toPackagePlanPayload(values: PackagePlanFormValues): PackagePlanPayload {
  return {
    ...values,
    cardLimit: toLimitOrNull(values.cardLimit),
    profileLimit: toLimitOrNull(values.profileLimit),
    reviewLocationLimit: toLimitOrNull(values.reviewLocationLimit),
    menuLimit: toLimitOrNull(values.menuLimit),
  };
}

export const productSchema = z.object({
  name: z.string().min(1, "Name is required").max(255),
  sku: z.string().min(1, "SKU is required").max(100),
  description: z.string().optional(),
  price: z.number({ error: "Enter a valid price" }).min(0, "Price must be 0 or more"),
  image: z.string().optional(),
  type: z.enum([
    "BUSINESS_CARD",
    "METAL_CARD",
    "GOOGLE_REVIEW_CARD",
    "REVIEW_STAND",
    "KEYCHAIN",
    "TABLE_TAG",
    "CUSTOM",
  ]),
  active: z.boolean(),
});

export type ProductFormValues = z.infer<typeof productSchema>;

export const orderCreateSchema = z.object({
  clientUuid: z.string().min(1, "Select a client"),
  items: z
    .array(
      z.object({
        productUuid: z.string().min(1, "Select a product"),
        quantity: z.number({ error: "Enter a valid quantity" }).int().min(1, "Minimum quantity is 1"),
      })
    )
    .min(1, "Add at least one item"),
  notes: z.string().optional(),
});

export type OrderCreateFormValues = z.infer<typeof orderCreateSchema>;

export const subscriptionAssignSchema = z.object({
  packagePlanUuid: z.string().min(1, "Select a plan"),
  status: z.enum(["TRIAL", "ACTIVE", "EXPIRED", "SUSPENDED", "CANCELLED"]),
  notes: z.string().optional(),
});

export type SubscriptionAssignFormValues = z.infer<typeof subscriptionAssignSchema>;
