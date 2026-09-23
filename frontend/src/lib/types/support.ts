export type TicketStatus = "OPEN" | "IN_PROGRESS" | "RESOLVED" | "CLOSED";
export type TicketPriority = "LOW" | "MEDIUM" | "HIGH";

export interface SupportMessage {
  uuid: string;
  senderEmail: string | null;
  fromSupportStaff: boolean;
  body: string;
  attachmentUrl: string | null;
  createdAt: string;
}

export interface SupportTicket {
  uuid: string;
  clientUuid: string | null;
  clientDisplayName: string | null;
  subject: string;
  status: TicketStatus;
  priority: TicketPriority;
  messages: SupportMessage[];
  createdAt: string;
  updatedAt: string;
}
