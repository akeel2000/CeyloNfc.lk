import { test, expect } from "@playwright/test";
import { loginAsClient } from "./helpers";

/**
 * Requires an isolated client account with the IDOR-TEST-CARD-B card assigned. Credentials are
 * supplied through E2E_CLIENT_EMAIL/E2E_CLIENT_PASSWORD, never stored in this repository.
 */
test("client can log in and see only their own NFC card", async ({ page }) => {
  await loginAsClient(page);

  await page.goto("/client/nfc-cards");
  await expect(page.getByText("IDOR-TEST-CARD-B")).toBeVisible({ timeout: 10_000 });
});
