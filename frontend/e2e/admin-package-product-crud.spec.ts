import { test, expect } from "@playwright/test";

import { loginAsAdmin } from "./helpers";

test("admin can create a package plan and see it appear in the list", async ({ page }) => {
  await loginAsAdmin(page);
  await page.goto("/admin/packages");

  const unique = Date.now();
  const name = `Playwright Package ${unique}`;

  await page.getByRole("button", { name: "Create Package" }).click();
  await page.getByLabel("Name").fill(name);
  await page.getByLabel("Price").fill("4999");
  await page.getByLabel("Sort order").fill("99");
  await page.getByRole("dialog").getByRole("button", { name: "Create Package" }).click();

  await expect(page.getByText("Package created")).toBeVisible({ timeout: 10_000 });
  await expect(page.getByRole("cell", { name })).toBeVisible({ timeout: 10_000 });
});

test("admin can add a product and see it appear in the list", async ({ page }) => {
  await loginAsAdmin(page);
  await page.goto("/admin/products");

  const unique = Date.now();
  const name = `Playwright Product ${unique}`;

  await page.getByRole("button", { name: "Add Product" }).click();
  await page.getByLabel("Name").fill(name);
  await page.getByLabel("SKU").fill(`PW-${unique}`);
  await page.getByLabel("Price").fill("1500");
  await page.getByRole("dialog").getByRole("button", { name: "Add Product" }).click();

  await expect(page.getByText("Product created")).toBeVisible({ timeout: 10_000 });
  await expect(page.getByRole("cell", { name })).toBeVisible({ timeout: 10_000 });
});
