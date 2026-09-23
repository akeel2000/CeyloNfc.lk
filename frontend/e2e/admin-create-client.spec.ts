import { test, expect } from "@playwright/test";
import { loginAsAdmin } from "./helpers";

test("admin can create a client and see it appear in the list", async ({ page }) => {
  await loginAsAdmin(page);

  await page.goto("/admin/clients");

  const unique = Date.now();
  const displayName = `Playwright Client ${unique}`;

  await page.getByRole("button", { name: "Create Client" }).click();
  await page.getByLabel("Display name").fill(displayName);
  await page.getByLabel("Email").fill(`playwright-${unique}@example.com`);
  await page.getByRole("dialog").getByRole("button", { name: "Create Client" }).click();

  // Show-once temporary password confirmation dialog.
  await expect(page.getByText("Client created")).toBeVisible({ timeout: 10_000 });
  await expect(page.getByText(/it will not be shown again/i)).toBeVisible();
  await page.getByRole("button", { name: "Done" }).click();

  await page.getByPlaceholder("Search name or email...").fill(displayName);
  await expect(page.getByRole("link", { name: displayName })).toBeVisible({ timeout: 10_000 });
});
