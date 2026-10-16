import { defineConfig, devices } from "@playwright/test";

// Separate opt-in suite: start the API with an isolated development database first.
if (process.env.LIVE_PROFILE_TESTS !== "true" || process.env.NEXT_PUBLIC_API_BASE_URL !== "http://localhost:8081") {
  throw new Error("Live profile tests require explicit opt-in and the isolated API on localhost:8081");
}
const port = Number(process.env.PLAYWRIGHT_PORT ?? 3101);
if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error("Invalid PLAYWRIGHT_PORT");
const origin = `http://localhost:${port}`;
export default defineConfig({
  testDir: "./tests/live", workers: 1, retries: 0, timeout: 60_000, expect: { timeout: 10_000 }, reporter: "list",
  use: { ...devices["Desktop Chrome"], baseURL: origin, viewport: { width: 1440, height: 1000 }, trace: "off", screenshot: "off", video: "off" },
  webServer: { command: `pnpm exec next start --hostname localhost --port ${port}`, url: origin, reuseExistingServer: false, timeout: 120_000 },
});
