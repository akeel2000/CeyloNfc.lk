import type { Page } from "@playwright/test";
import { expect } from "@playwright/test";

function requiredE2eEnv(name: string): string {
  const value = process.env[name];
  if (!value) throw new Error(`Missing ${name}. Copy .env.e2e.example to .env.e2e and provide isolated test credentials.`);
  return value;
}

export const e2eAdminEmail = () => requiredE2eEnv("E2E_ADMIN_EMAIL");

/** Shared by the admin-only journeys. Credentials must never be committed. */
export async function loginAsAdmin(page: Page) {
  await page.goto("/login");
  await page.getByLabel("Email").fill(e2eAdminEmail());
  await page.getByLabel("Password", { exact: true }).fill(requiredE2eEnv("E2E_ADMIN_PASSWORD"));
  await page.getByRole("button", { name: "Sign in" }).click();
  await expect(page).toHaveURL(/\/admin\/dashboard/, { timeout: 10_000 });
}

export async function loginAsClient(page: Page) {
  await page.goto("/login");
  await page.getByLabel("Email").fill(requiredE2eEnv("E2E_CLIENT_EMAIL"));
  await page.getByLabel("Password", { exact: true }).fill(requiredE2eEnv("E2E_CLIENT_PASSWORD"));
  await page.getByRole("button", { name: "Sign in" }).click();
  await expect(page).toHaveURL(/\/client\/dashboard/, { timeout: 10_000 });
}
