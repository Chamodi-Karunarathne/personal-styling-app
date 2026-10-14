import { defineConfig, devices } from "@playwright/test";

const unitOnly = process.argv.some((argument) => argument === "--project=unit");

export default defineConfig({
  timeout: 30_000,
  expect: { timeout: 7_000 },
  fullyParallel: false,
  workers: 2,
  retries: 0,
  reporter: "list",
  use: {
    baseURL: "http://localhost:3000",
    trace: "off",
    screenshot: "off",
    video: "off",
  },
  projects: [
    { name: "unit", testDir: "./tests/unit" },
    { name: "desktop", testDir: "./tests/auth", use: { ...devices["Desktop Chrome"], viewport: { width: 1440, height: 1000 } } },
    { name: "mobile", testDir: "./tests/auth", use: { ...devices["Pixel 7"], viewport: { width: 390, height: 844 } } },
  ],
  webServer: unitOnly ? undefined : {
    command: "pnpm exec next start --hostname localhost --port 3000",
    url: "http://localhost:3000",
    reuseExistingServer: false,
    timeout: 120_000,
  },
});
