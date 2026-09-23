import { test, expect } from "@playwright/test";
import { e2eAdminEmail, loginAsAdmin } from "./helpers";

test("admin can log in and land on the admin dashboard", async ({ page }) => {
  await loginAsAdmin(page);
  await expect(page.getByRole("link", { name: "Clients" })).toBeVisible();
});

test("rejects an invalid password with a visible error, no navigation", async ({ page }) => {
  await page.goto("/login");

  await page.getByLabel("Email").fill(e2eAdminEmail());
  await page.getByLabel("Password", { exact: true }).fill("definitely-wrong");
  await page.getByRole("button", { name: "Sign in" }).click();

  await expect(page.getByRole("alert")).toBeVisible({ timeout: 10_000 });
  await expect(page).toHaveURL(/\/login/);
});
