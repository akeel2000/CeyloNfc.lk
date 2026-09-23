import type { Metadata } from "next";

import { ProductsPageContent } from "@/features/products/products-page-content";

export const metadata: Metadata = { title: "Products" };

export default function AdminProductsPage() {
  return <ProductsPageContent />;
}
