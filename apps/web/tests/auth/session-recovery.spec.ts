import { expect, test } from "@playwright/test";
import { mockProfileBackend } from "../fixtures/profile-backend";
import { emptyProfile } from "../../src/lib/profile/types";

test("background verification failure hides both dirty editors and restores only the verified owner's drafts", async ({ page }) => {
  const backend = await mockProfileBackend(page); await page.goto("/profile");
  await page.getByLabel("Casual", { exact: true }).check();
  await page.getByLabel(/^Height \(cm\)/).fill("172");
  backend.failSessionRead = true; await page.evaluate(() => window.dispatchEvent(new Event("focus")));
  await expect(page.locator("main").getByRole("alert")).toHaveText("Unable to verify your session. Please try again.");
  await expect(page.getByRole("heading", { name: "Your styling profile", exact: true })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Save personal details", exact: true })).toHaveCount(0);
  expect(backend.writes).toHaveLength(0); expect(backend.appearanceWrites).toHaveLength(0);
  backend.failSessionRead = false; await page.getByRole("button", { name: "Try again", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Your styling profile", exact: true })).toBeVisible();
  await expect(page.getByLabel("Casual", { exact: true })).toBeChecked();
  await expect(page.getByLabel(/^Height \(cm\)/)).toHaveValue("172");
  backend.failSessionRead = true; await page.evaluate(() => window.dispatchEvent(new Event("focus")));
  await expect(page.locator("main").getByRole("alert")).toBeVisible();
  backend.account = { id: 8, fullName: "Another User", email: "other@example.com" }; backend.profile = emptyProfile();
  backend.failSessionRead = false; await page.getByRole("button", { name: "Try again", exact: true }).click();
  await expect(page.getByText("Another User", { exact: true })).toBeVisible();
  await expect(page.getByLabel("Casual", { exact: true })).not.toBeChecked();
  await expect(page.getByLabel(/^Height \(cm\)/)).toHaveValue("");
  expect(await page.evaluate(() => localStorage.length + sessionStorage.length)).toBe(0);
});

test("onboarding draft survives temporary verification failure but confirmed sign-out discards it", async ({ page }) => {
  const backend = await mockProfileBackend(page); await page.goto("/onboarding");
  await page.getByLabel("Casual", { exact: true }).check();
  backend.failSessionRead = true; await page.evaluate(() => window.dispatchEvent(new Event("focus")));
  await expect(page.locator("main").getByRole("alert")).toBeVisible();
  backend.failSessionRead = false; await page.getByRole("button", { name: "Try again", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Your style & colors", exact: true })).toBeVisible();
  await expect(page.getByLabel("Casual", { exact: true })).toBeChecked();
  backend.authenticated = false; await page.evaluate(() => window.dispatchEvent(new Event("focus")));
  await expect(page).toHaveURL(/\/login$/);
  await page.getByLabel("Email address", { exact: true }).fill(backend.account.email);
  await page.getByLabel("Password", { exact: true }).fill("SyntheticPassword123");
  await page.getByRole("button", { name: "Sign In", exact: true }).click();
  await expect(page).toHaveURL(/\/dashboard$/); await page.goto("/onboarding");
  await expect(page.getByLabel("Casual", { exact: true })).not.toBeChecked();
});
