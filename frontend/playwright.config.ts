import { defineConfig, devices } from "@playwright/test";

/**
 * Runs against the already-running local dev stack (backend on :8080, frontend on :3000) -
 * no webServer auto-start here since these E2E tests assume the same Docker+backend+frontend
 * stack used for manual verification throughout this project is already up (see
 * docs/PROJECT_PROGRESS.md "How to run it yourself").
 */
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [["list"]],
  use: {
    baseURL: "http://localhost:3000",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
});
