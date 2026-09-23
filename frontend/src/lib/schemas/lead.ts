import { z } from "zod";

export const leadCreateSchema = z.object({
  name: z.string().min(1, "Name is required").max(255),
  email: z.string().min(1, "Email is required").email("Enter a valid email"),
  phone: z.string().optional(),
  company: z.string().optional(),
  message: z.string().optional(),
});

export type LeadCreateFormValues = z.infer<typeof leadCreateSchema>;

export const leadUpdateSchema = z.object({
  status: z.enum(["NEW", "CONTACTED", "CONVERTED", "CLOSED"]),
  notes: z.string().optional(),
});

export type LeadUpdateFormValues = z.infer<typeof leadUpdateSchema>;
