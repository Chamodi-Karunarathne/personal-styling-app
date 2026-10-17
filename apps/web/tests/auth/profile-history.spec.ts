import { expect, test } from "@playwright/test";
import { mockProfileBackend } from "../fixtures/profile-backend";
import { emptyProfile } from "../../src/lib/profile/types";

test("Back and Forward retain dirty styling and appearance without changing history or saving", async ({ page }) => {
  const backend = await mockProfileBackend(page); await page.goto("/dashboard");
  await page.getByRole("link", { name: "Edit preferences", exact: true }).click();
  await page.getByLabel("Casual", { exact: true }).check(); await page.getByLabel(/^Height \(cm\)/).fill("172");
  await page.goBack(); await expect(page).toHaveURL(/\/dashboard$/);
  await page.goForward(); await expect(page).toHaveURL(/\/profile$/);
  await expect(page.getByLabel("Casual", { exact: true })).toBeChecked();
  await expect(page.getByLabel(/^Height \(cm\)/)).toHaveValue("172");
  expect(backend.writes).toHaveLength(0); expect(backend.appearanceWrites).toHaveLength(0);
  page.once("dialog", (dialog) => dialog.dismiss()); await page.getByRole("link", { name: "Back to dashboard", exact: true }).click();
  await expect(page).toHaveURL(/\/profile$/);
  page.once("dialog", (dialog) => dialog.accept()); await page.getByRole("link", { name: "Back to dashboard", exact: true }).click();
  await expect(page).toHaveURL(/\/dashboard$/); await page.goBack();
  await expect(page.getByLabel("Casual", { exact: true })).not.toBeChecked();
  await expect(page.getByLabel(/^Height \(cm\)/)).toHaveValue("");
  expect(await page.evaluate(() => localStorage.length + sessionStorage.length)).toBe(0);
});

test("dirty onboarding restores its step and edits after browser history navigation", async ({ page }) => {
  await mockProfileBackend(page); await page.goto("/dashboard"); await page.getByRole("link", { name: "Start setup", exact: true }).click();
  await page.getByLabel("Casual", { exact: true }).check(); await page.goBack(); await expect(page).toHaveURL(/\/dashboard$/);
  await page.goForward(); await expect(page.getByLabel("Casual", { exact: true })).toBeChecked();
  for (let i = 0; i < 3; i++) await page.getByRole("button", { name: "Next", exact: true }).click();
  await page.getByLabel(/^Height \(cm\)/).fill("172"); await page.goBack(); await expect(page).toHaveURL(/\/dashboard$/);
  await page.goForward(); await expect(page.getByRole("heading", { name: "Personal details and fit", exact: true })).toBeVisible();
  await expect(page.getByLabel(/^Height \(cm\)/)).toHaveValue("172");
});

test("returning to a dirty personal step retains its original revision and rejects concurrent changes", async ({ page }) => {
  const backend = await mockProfileBackend(page);
  backend.profile = { ...emptyProfile(), version: 0, onboardingStatus: "IN_PROGRESS", lastVisitedStep: "REVIEW", appearanceStepPending: true, updatedAt: "2026-10-17T00:00:00Z" };
  await page.goto("/dashboard"); await page.getByRole("link", { name: "Resume setup", exact: true }).click();
  await page.getByLabel(/^Height \(cm\)/).fill("172"); await page.goBack(); await expect(page).toHaveURL(/\/dashboard$/);
  backend.appearance = { ...backend.appearance, version: crypto.randomUUID(), updatedAt: "2026-10-17T00:00:00Z", details: { ...backend.appearance.details, heightCm: 180 } };
  await page.goForward(); await expect(page.getByLabel(/^Height \(cm\)/)).toHaveValue("172");
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(page.locator("main").getByRole("alert")).toContainText("personal details changed elsewhere");
  expect(backend.appearanceWrites[0].version).toBeNull(); expect(backend.appearance.details.heightCm).toBe(180);
});

test("logout clears history drafts even when the same account signs in again", async ({ page }) => {
  const backend = await mockProfileBackend(page); await page.goto("/dashboard");
  await page.getByRole("link", { name: "Edit preferences", exact: true }).click();
  await page.getByLabel("Casual", { exact: true }).check(); await page.getByLabel(/^Height \(cm\)/).fill("172");
  await page.goBack(); await expect(page).toHaveURL(/\/dashboard$/); await page.getByRole("button", { name: "Sign out", exact: true }).click();
  await expect(page).toHaveURL(/\/login$/); await page.getByLabel("Email address", { exact: true }).fill(backend.account.email);
  await page.getByLabel("Password", { exact: true }).fill("SyntheticPassword123"); await page.getByRole("button", { name: "Sign In", exact: true }).click();
  await expect(page).toHaveURL(/\/dashboard$/); await page.getByRole("link", { name: "Edit preferences", exact: true }).click();
  await expect(page.getByLabel("Casual", { exact: true })).not.toBeChecked(); await expect(page.getByLabel(/^Height \(cm\)/)).toHaveValue("");
});

test("clean history navigation fetches current saved data without confirmation", async ({ page }) => {
  const backend = await mockProfileBackend(page); page.on("dialog", () => { throw new Error("Clean navigation must not prompt"); });
  await page.goto("/dashboard"); await page.getByRole("link", { name: "Edit preferences", exact: true }).click();
  await expect(page.getByLabel("Casual", { exact: true })).not.toBeChecked(); await page.goBack(); await expect(page).toHaveURL(/\/dashboard$/);
  backend.profile = { ...emptyProfile(), version: 0, onboardingStatus: "IN_PROGRESS", updatedAt: "2026-10-17T00:00:00Z", preferences: { ...emptyProfile().preferences, styles: ["CASUAL"] } };
  await page.goForward(); await expect(page.getByLabel("Casual", { exact: true })).toBeChecked();
  await page.goBack(); await expect(page).toHaveURL(/\/dashboard$/);
});

test("leaving during a write restores edits with an uncertainty lock instead of replaying it", async ({ page }) => {
  const backend = await mockProfileBackend(page); await page.goto("/dashboard");
  await page.getByRole("link", { name: "Edit preferences", exact: true }).click(); await page.getByLabel("Casual", { exact: true }).check();
  let release!: () => void; const gate = new Promise<void>((resolve) => { release = resolve; });
  let dispatched!: () => void; const started = new Promise<void>((resolve) => { dispatched = resolve; });
  backend.before = async (path, method) => { if (path === "/api/profile/me" && method === "PUT") { dispatched(); await gate; } };
  try {
    await page.getByRole("button", { name: "Save preferences", exact: true }).click(); await started;
    await page.goBack(); await expect(page).toHaveURL(/\/dashboard$/); release();
    await page.goForward(); await expect(page.getByLabel("Casual", { exact: true })).toBeChecked();
    await expect(page.locator("main").getByRole("alert")).toContainText("in progress when you left");
    await expect(page.getByRole("button", { name: "Save preferences", exact: true })).toBeDisabled();
    expect(backend.writes.length).toBeLessThanOrEqual(1);
  } finally { release(); }
});
