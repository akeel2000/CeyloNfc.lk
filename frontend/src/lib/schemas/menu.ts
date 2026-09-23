import { z } from "zod";

export const menuUpdateSchema = z.object({
  slug: z
    .string()
    .min(1, "URL slug is required")
    .regex(/^[a-z0-9]+(-[a-z0-9]+)*$/, "Lowercase letters, numbers and hyphens only"),
  name: z.string().min(1, "Name is required"),
  description: z.string().optional(),
  logo: z.string().optional(),
  currency: z.string().min(1).max(10),
});

export type MenuUpdateFormValues = z.infer<typeof menuUpdateSchema>;

export const menuCategorySchema = z.object({
  name: z.string().min(1, "Category name is required"),
});

export type MenuCategoryFormValues = z.infer<typeof menuCategorySchema>;

export const menuItemSchema = z.object({
  name: z.string().min(1, "Item name is required"),
  description: z.string().optional(),
  image: z.string().optional(),
  price: z.number({ error: "Enter a valid price" }).min(0, "Price must be 0 or more"),
  available: z.boolean().optional(),
  featured: z.boolean().optional(),
});

export type MenuItemFormValues = z.infer<typeof menuItemSchema>;
