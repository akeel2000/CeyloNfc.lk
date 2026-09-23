export type QrCodeStatus = "ACTIVE" | "SUSPENDED";

export interface QrCode {
  uuid: string;
  name: string;
  status: QrCodeStatus;
  destinationUuid: string | null;
  destinationName: string | null;
  totalScans: number;
  lastScannedAt: string | null;
  createdAt: string;
}

export interface QrCodeCreateResult {
  qrCode: QrCode;
  publicUrl: string;
}
