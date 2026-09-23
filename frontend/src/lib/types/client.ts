export type ClientType = "INDIVIDUAL" | "BUSINESS";
export type ClientStatus = "PENDING" | "ACTIVE" | "SUSPENDED" | "CLOSED";

export interface Client {
  uuid: string;
  type: ClientType;
  status: ClientStatus;
  displayName: string;
  email: string;
  phone: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ClientCreateResult {
  client: Client;
  temporaryPassword: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
