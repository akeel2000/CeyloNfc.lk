import { test, expect } from "@playwright/test";
import { loginAsClient } from "./helpers";

test("client can open a support ticket end-to-end through the UI", async ({ page }) => {
  await loginAsClient(page);

  await page.goto("/client/support");

  const subject = `Playwright E2E ticket ${Date.now()}`;

  await page.getByRole("button", { name: "New Ticket" }).click();
  await page.getByLabel("Subject").fill(subject);
  await page.getByLabel("Message").fill("Filed automatically by the Playwright E2E suite.");
  await page.getByRole("dialog").getByRole("button", { name: "Create Ticket" }).click();

  await expect(page.getByRole("link", { name: subject })).toBeVisible({ timeout: 10_000 });

  await page.getByRole("link", { name: subject }).click();
  await expect(page).toHaveURL(/\/client\/support\/[\w-]+/, { timeout: 10_000 });
  await expect(page.getByText("Filed automatically by the Playwright E2E suite.")).toBeVisible({ timeout: 10_000 });
});
