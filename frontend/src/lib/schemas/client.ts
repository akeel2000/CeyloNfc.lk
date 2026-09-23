import { z } from "zod";

export const clientCreateSchema = z.object({
  type: z.enum(["INDIVIDUAL", "BUSINESS"]),
  displayName: z.string().min(1, "Display name is required").max(255),
  email: z.string().min(1, "Email is required").email("Enter a valid email"),
  phone: z.string().optional(),
});

export type ClientCreateFormValues = z.infer<typeof clientCreateSchema>;

export const clientUpdateSchema = z.object({
  displayName: z.string().min(1, "Display name is required").max(255),
  email: z.string().min(1, "Email is required").email("Enter a valid email"),
  phone: z.string().optional(),
});

export type ClientUpdateFormValues = z.infer<typeof clientUpdateSchema>;
