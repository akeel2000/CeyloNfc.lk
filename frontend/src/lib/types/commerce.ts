export type BillingPeriod = "MONTHLY" | "YEARLY" | "ONE_TIME";

export interface PackagePlan {
  uuid: string;
  name: string;
  description: string | null;
  price: number;
  billingPeriod: BillingPeriod;
  cardLimit: number | null;
  profileLimit: number | null;
  reviewLocationLimit: number | null;
  menuLimit: number | null;
  premiumTemplates: boolean;
  active: boolean;
  sortOrder: number;
}

export type SubscriptionStatus = "TRIAL" | "ACTIVE" | "EXPIRED" | "SUSPENDED" | "CANCELLED";

export interface Subscription {
  uuid: string;
  clientUuid: string | null;
  clientDisplayName: string | null;
  status: SubscriptionStatus;
  startDate: string;
  endDate: string | null;
  renewalDate: string | null;
  notes: string | null;
  plan: PackagePlan;
  cardsUsed: number;
}

export type ProductType =
  | "BUSINESS_CARD"
  | "METAL_CARD"
  | "GOOGLE_REVIEW_CARD"
  | "REVIEW_STAND"
  | "KEYCHAIN"
  | "TABLE_TAG"
  | "CUSTOM";

export interface Product {
  uuid: string;
  name: string;
  sku: string;
  description: string | null;
  price: number;
  image: string | null;
  type: ProductType;
  active: boolean;
}

export type OrderStatus =
  | "NEW"
  | "CONFIRMED"
  | "DESIGNING"
  | "PRINTING"
  | "PROGRAMMING_NFC"
  | "READY"
  | "SHIPPED"
  | "DELIVERED"
  | "CANCELLED";

export type PaymentStatus = "UNPAID" | "PARTIALLY_PAID" | "PAID" | "REFUNDED";

export interface OrderItem {
  productUuid: string | null;
  productName: string;
  quantity: number;
  unitPrice: number;
}

export interface Order {
  uuid: string;
  orderNumber: string;
  clientUuid: string | null;
  clientDisplayName: string | null;
  status: OrderStatus;
  paymentStatus: PaymentStatus;
  subtotal: number;
  total: number;
  notes: string | null;
  items: OrderItem[];
  createdAt: string;
}
