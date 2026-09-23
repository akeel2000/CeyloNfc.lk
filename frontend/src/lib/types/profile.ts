export type SocialPlatform =
  | "FACEBOOK"
  | "INSTAGRAM"
  | "LINKEDIN"
  | "TIKTOK"
  | "YOUTUBE"
  | "X"
  | "TELEGRAM"
  | "WHATSAPP"
  | "CUSTOM";

export interface SocialLink {
  platform: SocialPlatform;
  url: string;
  displayOrder: number;
  enabled: boolean;
}

/** dayOfWeek matches java.time.DayOfWeek.getValue() (1=Monday .. 7=Sunday). opensAt/closesAt
 *  are "HH:mm" strings, both null when closed is true. Company/business profiles only. */
export interface BusinessHour {
  dayOfWeek: number;
  opensAt: string | null;
  closesAt: string | null;
  closed: boolean;
}

export interface Profile {
  type: "INDIVIDUAL" | "BUSINESS";
  uuid: string;
  slug: string;
  published: boolean;
  publicUrl: string;

  fullName: string | null;
  jobTitle: string | null;

  companyName: string | null;
  industry: string | null;
  registrationNumber: string | null;
  googleMapsUrl: string | null;
  logo: string | null;

  bio: string | null;
  profileImage: string | null;
  coverImage: string | null;
  phone: string | null;
  whatsapp: string | null;
  email: string | null;
  website: string | null;
  address: string | null;
  city: string | null;
  country: string | null;
  templateUuid: string | null;

  socialLinks: SocialLink[];
  /** Company-only - always empty for INDIVIDUAL. */
  businessHours: BusinessHour[];
}

export interface PublicProfile {
  type: "INDIVIDUAL" | "BUSINESS";
  slug: string;
  fullName: string | null;
  jobTitle: string | null;
  companyName: string | null;
  industry: string | null;
  bio: string | null;
  profileImage: string | null;
  coverImage: string | null;
  logo: string | null;
  phone: string | null;
  whatsapp: string | null;
  email: string | null;
  website: string | null;
  address: string | null;
  city: string | null;
  country: string | null;
  googleMapsUrl: string | null;
  templatePrimaryColor: string | null;
  templateLayout: "CLASSIC" | "MINIMAL" | null;
  socialLinks: SocialLink[];
  /** Company-only - null for INDIVIDUAL. */
  businessHours: BusinessHour[] | null;
  openNow: boolean | null;
  openStatusLabel: string | null;
}
