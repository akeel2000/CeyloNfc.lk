export type NfcCardStatus = "UNASSIGNED" | "ACTIVE" | "INACTIVE" | "SUSPENDED" | "LOST" | "EXPIRED" | "REPLACED";

export interface NfcCard {
  uuid: string;
  serialNumber: string;
  status: NfcCardStatus;
  clientUuid: string | null;
  clientDisplayName: string | null;
  destinationUuid: string | null;
  destinationName: string | null;
  notes: string | null;
  activatedAt: string | null;
  lastTappedAt: string | null;
  totalTaps: number;
  createdAt: string;
}

export interface NfcCardRegisterResult {
  card: NfcCard;
  rawToken: string;
  publicUrl: string;
}

export type DestinationType = "WEBSITE" | "WHATSAPP" | "SOCIAL" | "CUSTOM_URL";

export interface Destination {
  uuid: string;
  name: string;
  type: DestinationType;
  externalUrl: string | null;
  active: boolean;
}
