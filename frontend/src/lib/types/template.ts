export type TemplateLayout = "CLASSIC" | "MINIMAL";

export interface Template {
  uuid: string;
  name: string;
  description: string | null;
  previewImage: string | null;
  primaryColor: string;
  layout: TemplateLayout;
  premium: boolean;
  active: boolean;
  sortOrder: number;
}

export interface TemplateGalleryItem {
  template: Template;
  locked: boolean;
  selected: boolean;
}
