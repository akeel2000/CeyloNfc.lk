export interface MenuItem {
  uuid: string;
  name: string;
  description: string | null;
  image: string | null;
  price: number;
  available: boolean;
  featured: boolean;
  sortOrder: number;
}

export interface MenuCategory {
  uuid: string;
  name: string;
  sortOrder: number;
  active: boolean;
  items: MenuItem[];
}

export interface Menu {
  uuid: string;
  slug: string;
  name: string;
  description: string | null;
  logo: string | null;
  currency: string;
  published: boolean;
  publicUrl: string;
  categories: MenuCategory[];
}
