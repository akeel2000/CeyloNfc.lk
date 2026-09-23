import { z } from "zod";

export const ticketCreateSchema = z.object({
  subject: z.string().min(1, "Subject is required").max(255),
  message: z.string().min(1, "Please describe the issue"),
  priority: z.enum(["LOW", "MEDIUM", "HIGH"]),
});

export type TicketCreateFormValues = z.infer<typeof ticketCreateSchema>;

export const messageCreateSchema = z.object({
  body: z.string().min(1, "Message can't be empty"),
});

export type MessageCreateFormValues = z.infer<typeof messageCreateSchema>;
