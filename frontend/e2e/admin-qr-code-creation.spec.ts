import { test, expect } from "@playwright/test";

import { loginAsAdmin } from "./helpers";

test("admin can create a QR code for a client and see it appear in their list", async ({ page }) => {
  await loginAsAdmin(page);

  // A fresh client, created the same way admin-create-client.spec.ts does, so this test doesn't
  // depend on any pre-seeded data existing in the dev database.
  await page.goto("/admin/clients");
  const unique = Date.now();
  const displayName = `Playwright QR Client ${unique}`;

  await page.getByRole("button", { name: "Create Client" }).click();
  await page.getByLabel("Display name").fill(displayName);
  await page.getByLabel("Email").fill(`playwright-qr-${unique}@example.com`);
  await page.getByRole("dialog").getByRole("button", { name: "Create Client" }).click();
  await expect(page.getByText("Client created")).toBeVisible({ timeout: 10_000 });
  await page.getByRole("button", { name: "Done" }).click();

  await page.goto("/admin/qr-codes");
  await page.getByRole("combobox").filter({ hasText: "Select a client" }).click();
  await page.getByRole("option", { name: displayName }).click();

  const qrName = `Playwright QR ${unique}`;
  await page.getByRole("button", { name: "Create QR Code" }).click();
  await page.getByLabel("Name").fill(qrName);
  await page.getByLabel("URL").fill("https://example.com/playwright-e2e");
  await page.getByRole("dialog").getByRole("button", { name: "Create", exact: true }).click();

  await expect(page.getByText("QR code created")).toBeVisible({ timeout: 10_000 });
  await page.getByRole("button", { name: "Done" }).click();

  await expect(page.getByText(qrName, { exact: true }).first()).toBeVisible({ timeout: 10_000 });
});
