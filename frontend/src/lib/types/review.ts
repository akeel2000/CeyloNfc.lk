export interface GoogleReviewLocation {
  uuid: string;
  businessName: string;
  locationName: string | null;
  address: string | null;
  googleMapsUrl: string | null;
  googleReviewUrl: string;
  googlePlaceId: string | null;
  active: boolean;
}
