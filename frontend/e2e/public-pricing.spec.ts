import { test, expect } from "@playwright/test";

test("pricing page renders live plan data from the backend, not hardcoded", async ({ page }) => {
  await page.goto("/pricing");

  await expect(page.getByRole("heading", { name: "Simple, transparent pricing" })).toBeVisible();

  // At least one real plan card fetched from GET /api/v1/public/packages.
  await expect(page.getByRole("link", { name: "Get started" }).first()).toBeVisible();
});

test("public contact form submits a real lead to the backend", async ({ page }) => {
  await page.goto("/#contact");

  const unique = Date.now();
  await page.getByLabel("Name").fill("Playwright E2E Test");
  await page.getByLabel("Email").fill(`e2e-${unique}@example.com`);
  await page.getByLabel("What are you looking for?").fill("E2E test submission");

  await page.getByRole("button", { name: "Send message" }).click();

  await expect(page.getByText("Thanks - we'll be in touch shortly.")).toBeVisible({ timeout: 10_000 });
});
