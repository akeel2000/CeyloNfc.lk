import { test, expect } from "@playwright/test";

import { loginAsAdmin } from "./helpers";

test("admin can create an order for a client against a real product", async ({ page }) => {
  await loginAsAdmin(page);
  const unique = Date.now();

  // A fresh product and client, created the same way the other CRUD specs do, so this test
  // doesn't depend on any pre-seeded data existing in the dev database.
  await page.goto("/admin/products");
  const productName = `Playwright Order Product ${unique}`;
  await page.getByRole("button", { name: "Add Product" }).click();
  await page.getByLabel("Name").fill(productName);
  await page.getByLabel("SKU").fill(`PW-ORD-${unique}`);
  await page.getByLabel("Price").fill("2500");
  await page.getByRole("dialog").getByRole("button", { name: "Add Product" }).click();
  await expect(page.getByText("Product created")).toBeVisible({ timeout: 10_000 });

  await page.goto("/admin/clients");
  const displayName = `Playwright Order Client ${unique}`;
  await page.getByRole("button", { name: "Create Client" }).click();
  await page.getByLabel("Display name").fill(displayName);
  await page.getByLabel("Email").fill(`playwright-order-${unique}@example.com`);
  await page.getByRole("dialog").getByRole("button", { name: "Create Client" }).click();
  await expect(page.getByText("Client created")).toBeVisible({ timeout: 10_000 });
  await page.getByRole("button", { name: "Done" }).click();

  await page.goto("/admin/orders");
  await page.getByRole("button", { name: "Create Order" }).click();
  await page.getByRole("combobox").filter({ hasText: "Select a client" }).click();
  await page.getByRole("option", { name: displayName }).click();
  await page.getByRole("combobox").filter({ hasText: "Select a product" }).click();
  await page.getByRole("option", { name: new RegExp(productName) }).click();
  await page.getByRole("dialog").getByRole("button", { name: "Create Order" }).click();

  await expect(page.getByText("Order created")).toBeVisible({ timeout: 10_000 });
  await expect(page.getByRole("cell", { name: displayName })).toBeVisible({ timeout: 10_000 });
});
