import { defineConfig, devices } from "@playwright/test";

const unitOnly = process.argv.some((argument) => argument === "--project=unit");
const port = Number(process.env.PLAYWRIGHT_PORT ?? 3000);
if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error("Invalid PLAYWRIGHT_PORT");
const origin = `http://localhost:${port}`;

export default defineConfig({
  timeout: 30_000,
  expect: { timeout: 7_000 },
  fullyParallel: false,
  workers: 2,
  retries: 0,
  reporter: "list",
  use: {
    baseURL: origin,
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
    command: `pnpm exec next start --hostname localhost --port ${port}`,
    url: origin,
    reuseExistingServer: false,
    timeout: 120_000,
  },
});
