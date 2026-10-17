import { expect, test } from "@playwright/test";
import { mockProfileBackend } from "../fixtures/profile-backend";
import { emptyProfile } from "../../src/lib/profile/types";

type Backend = Awaited<ReturnType<typeof mockProfileBackend>>;
function atPersonal(backend: Backend) {
  backend.profile = { ...emptyProfile(), version: 0, onboardingStatus: "IN_PROGRESS", lastVisitedStep: "REVIEW", appearanceStepPending: true, updatedAt: "2026-10-17T00:00:00Z" };
}
test("fifth-step progress saves personal details and restores the personal step after Save and exit", async ({ page }) => {
  const backend = await mockProfileBackend(page); atPersonal(backend);
  await page.goto("/onboarding");
  await expect(page.getByRole("heading", { name: "Personal details and fit", exact: true })).toBeVisible();
  await expect(page.getByText("Step 4 of 5", { exact: true })).toBeVisible();
  await page.getByLabel(/^Height \(cm\)/).fill("172");
  await page.getByRole("button", { name: "Save and exit", exact: true }).click();
  await expect(page).toHaveURL(/\/dashboard$/); expect(backend.appearance.details.heightCm).toBe(172);
  await page.getByRole("link", { name: "Resume setup", exact: true }).click();
  await expect(page.getByLabel(/^Height \(cm\)/)).toHaveValue("172");
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Review your preferences", exact: true })).toBeVisible();
  await expect(page.getByText("172 cm", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Finish onboarding", exact: true }).click();
  await expect(page).toHaveURL(/\/dashboard$/); expect(backend.profile.onboardingStatus).toBe("COMPLETED");
  expect(backend.appearanceWrites).toHaveLength(1); expect(backend.profile.appearanceStepPending).toBe(false);
});
test("legacy Review drafts and completed accounts do not require the new optional step", async ({ page }) => {
  const backend = await mockProfileBackend(page); atPersonal(backend); delete backend.profile.appearanceStepPending;
  await page.goto("/onboarding");
  await expect(page.getByRole("heading", { name: "Review your preferences", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Finish onboarding", exact: true }).click();
  await expect(page).toHaveURL(/\/dashboard$/); await expect(page.getByRole("link", { name: "Resume setup", exact: true })).toHaveCount(0);
  await expect(page.getByRole("link", { name: "Edit preferences", exact: true })).toBeVisible();
  expect(backend.profile.onboardingStatus).toBe("COMPLETED"); expect(backend.appearanceWrites).toHaveLength(0);
});
test("skipping personal details preserves saved values and discards only confirmed local changes", async ({ page }) => {
  const backend = await mockProfileBackend(page); atPersonal(backend);
  backend.appearance = { ...backend.appearance, version: "11111111-1111-4111-8111-111111111111", updatedAt: "2026-10-17T00:00:00Z", details: { ...backend.appearance.details, heightCm: 172 } };
  await page.goto("/onboarding"); await page.getByLabel(/^Height \(cm\)/).fill("173");
  page.once("dialog", (dialog) => dialog.accept()); await page.getByRole("button", { name: "Skip this step", exact: true }).click();
  await expect(page.getByText("172 cm", { exact: true })).toBeVisible();
  expect(backend.appearance.details.heightCm).toBe(172); expect(backend.appearanceWrites).toHaveLength(0);
});
test("partial save success is explicit and recovery never repeats the already saved personal write", async ({ page }) => {
  const backend = await mockProfileBackend(page); atPersonal(backend);
  await page.goto("/onboarding"); await page.getByLabel(/^Height \(cm\)/).fill("172");
  backend.failWrite = { status: 409 }; await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(page.getByRole("main").getByRole("alert")).toContainText("Personal details were saved. We couldn't confirm onboarding progress.");
  expect(backend.appearanceWrites).toHaveLength(1); expect(backend.profile.appearanceStepPending).toBe(true);
  await expect(page.getByLabel(/^Height \(cm\)/)).toHaveValue("172");
  page.once("dialog", (dialog) => dialog.accept()); await page.getByRole("button", { name: "Reload saved profile", exact: true }).click();
  await expect(page.getByLabel(/^Height \(cm\)/)).toHaveValue("172");
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Review your preferences", exact: true })).toBeVisible(); expect(backend.appearanceWrites).toHaveLength(1);
});
test("an unavailable optional resource can be skipped and completion does not modify it", async ({ page }) => {
  const backend = await mockProfileBackend(page); atPersonal(backend); backend.failAppearanceRead = true;
  await page.goto("/onboarding"); await expect(page.getByRole("main").getByRole("alert")).toBeVisible();
  await page.getByRole("button", { name: "Skip this step", exact: true }).click();
  await expect(page.getByText("Personal details are not loaded. Saved personal details have not been changed.", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Finish onboarding", exact: true }).click();
  await expect(page).toHaveURL(/\/dashboard$/); expect(backend.appearanceWrites).toHaveLength(0);
});
test("personal validation blocks both resources and Back keeps unsaved choices", async ({ page }) => {
  const backend = await mockProfileBackend(page); atPersonal(backend);
  await page.setViewportSize({ width: 360, height: 850 }); await page.goto("/onboarding");
  await page.getByLabel(/^Height \(cm\)/).fill("172.5"); await page.getByRole("button", { name: "Back", exact: true }).click();
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(page.getByLabel(/^Height \(cm\)/)).toHaveValue("172.5");
  await expect(page.getByRole("main").getByRole("alert")).toBeFocused(); expect(backend.appearanceWrites).toHaveLength(0); expect(backend.writes).toHaveLength(0);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});
