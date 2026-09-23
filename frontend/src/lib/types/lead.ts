import type { ClientCreateResult } from "@/lib/types/client";

export type LeadStatus = "NEW" | "CONTACTED" | "CONVERTED" | "CLOSED";

export interface Lead {
  uuid: string;
  name: string;
  email: string;
  phone: string | null;
  company: string | null;
  message: string | null;
  source: string | null;
  status: LeadStatus;
  notes: string | null;
  createdAt: string;
}

export interface LeadConvertResult {
  lead: Lead;
  client: ClientCreateResult;
}
