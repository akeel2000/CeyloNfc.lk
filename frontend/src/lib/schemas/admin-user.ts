import { z } from "zod";

export const adminUserCreateSchema = z.object({
  email: z.string().min(1, "Email is required").email("Enter a valid email"),
  phone: z.string().optional(),
  role: z.enum(["ADMIN", "SUPER_ADMIN"]),
});

export type AdminUserCreateFormValues = z.infer<typeof adminUserCreateSchema>;
