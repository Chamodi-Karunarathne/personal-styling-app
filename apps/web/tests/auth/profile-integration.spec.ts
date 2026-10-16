import { expect, test } from "@playwright/test";
import { mockProfileBackend } from "../fixtures/profile-backend";
import { emptyProfile } from "../../src/lib/profile/types";

test("profile loading failure is retryable and never invents an empty profile", async ({ page }) => {
  const backend = await mockProfileBackend(page); backend.failRead = true;
  await page.goto("/profile");
  await expect(page.getByRole("main").getByRole("alert")).toContainText("Something went wrong");
  await expect(page.getByLabel("Casual", { exact: true })).toHaveCount(0);
  backend.failRead = false;
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  await expect(page.getByLabel("Casual", { exact: true })).toBeVisible();
});

test("conflict preserves edits, blocks resubmission, and reload requires explicit confirmation", async ({ page }) => {
  const backend = await mockProfileBackend(page);
  await page.goto("/profile");
  await page.getByLabel("Casual", { exact: true }).check();
  backend.failWrite = { status: 409 };
  await page.getByRole("button", { name: "Save preferences", exact: true }).click();
  await expect(page.getByRole("main").getByRole("alert")).toContainText("preferences changed elsewhere");
  await expect(page.getByLabel("Casual", { exact: true })).toBeChecked();
  await expect(page.getByRole("button", { name: "Save preferences", exact: true })).toBeDisabled();
  page.once("dialog", (dialog) => dialog.accept());
  await page.getByRole("button", { name: "Reload saved profile", exact: true }).click();
  await expect(page.getByLabel("Casual", { exact: true })).not.toBeChecked();
  expect(backend.writes).toHaveLength(1);
});

test("HTTP field validation returns onboarding to the affected step without losing edits", async ({ page }) => {
  const backend = await mockProfileBackend(page);
  await page.goto("/onboarding");
  await page.getByLabel("Casual", { exact: true }).check();
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(page.getByRole("heading", { name: "What feels comfortable", exact: true })).toBeVisible();
  backend.failWrite = { status: 400, fieldErrors: { "preferences.styles": "Choose a supported option" } };
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Your style & colors", exact: true })).toBeVisible();
  await expect(page.getByText("Choose a supported option", { exact: true })).toBeVisible();
  await expect(page.getByLabel("Casual", { exact: true })).toBeChecked();
});

test("lost successful PUT response is confirmed by GET without duplicate submission", async ({ page }) => {
  const backend = await mockProfileBackend(page);
  await page.goto("/profile");
  await page.getByLabel("Casual", { exact: true }).check();
  backend.failWrite = { loseResponse: true, commit: true };
  await page.getByRole("button", { name: "Save preferences", exact: true }).click();
  await expect(page.getByRole("status")).toContainText("have been saved");
  expect(backend.writes).toHaveLength(1);
});

test("unconfirmed PUT outcome preserves draft and requires reload rather than blind retry", async ({ page }) => {
  const backend = await mockProfileBackend(page);
  await page.goto("/profile");
  await page.getByLabel("Casual", { exact: true }).check();
  backend.failWrite = { loseResponse: true, commit: false };
  await page.getByRole("button", { name: "Save preferences", exact: true }).click();
  await expect(page.getByRole("main").getByRole("alert")).toContainText("couldn't confirm");
  await expect(page.getByLabel("Casual", { exact: true })).toBeChecked();
  await expect(page.getByRole("button", { name: "Save preferences", exact: true })).toBeDisabled();
  expect(backend.writes).toHaveLength(1);
});

test("expired profile session returns to sign in and does not send a PUT", async ({ page }) => {
  const backend = await mockProfileBackend(page);
  await page.goto("/profile");
  await page.getByLabel("Casual", { exact: true }).check();
  backend.authenticated = false;
  await page.getByRole("button", { name: "Save preferences", exact: true }).click();
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole("status")).toContainText("session has expired");
  expect(backend.writes).toHaveLength(0);
});

test("canceling a conflicted edit does not bypass the required reload", async ({ page }) => {
  const backend = await mockProfileBackend(page);
  await page.goto("/profile");
  await page.getByLabel("Casual", { exact: true }).check();
  backend.failWrite = { status: 409 };
  await page.getByRole("button", { name: "Save preferences", exact: true }).click();
  await expect(page.getByRole("button", { name: "Save preferences", exact: true })).toBeDisabled();
  page.once("dialog", (dialog) => dialog.accept());
  await page.getByRole("button", { name: "Cancel", exact: true }).click();
  await expect(page.getByRole("button", { name: "Save preferences", exact: true })).toBeDisabled();
  await expect(page.getByRole("button", { name: "Reload saved profile", exact: true })).toBeVisible();
});

test("signing out clears account A's profile before account B edits preferences", async ({ page }) => {
  const backend = await mockProfileBackend(page);
  backend.profile = { ...emptyProfile(), version: 1, onboardingStatus: "IN_PROGRESS", updatedAt: "2026-10-17T00:00:00Z",
    preferences: { ...emptyProfile().preferences, styles: ["CASUAL"] } };
  await page.goto("/profile");
  await expect(page.getByLabel("Casual", { exact: true })).toBeChecked();
  await page.getByRole("link", { name: "Back to dashboard", exact: true }).click();
  await page.getByRole("button", { name: "Sign out", exact: true }).click();
  await expect(page).toHaveURL(/\/login$/);
  backend.account = { id: 8, fullName: "Second User", email: "second@example.com" }; backend.profile = emptyProfile();
  await page.getByLabel("Email address", { exact: true }).fill(backend.account.email);
  await page.getByLabel("Password", { exact: true }).fill("SyntheticSecond123");
  await page.getByRole("button", { name: "Sign In", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Welcome, Second User", exact: true })).toBeVisible();
  await page.getByRole("link", { name: "Edit preferences", exact: true }).click();
  await expect(page.getByText("Second User", { exact: true })).toBeVisible();
  await expect(page.getByLabel("Casual", { exact: true })).not.toBeChecked();
});

test("setup and profile layouts fit the supported viewport checks", async ({ page }) => {
  await mockProfileBackend(page);
  for (const width of [1440, 1024, 768, 390, 360]) {
    await page.setViewportSize({ width, height: 900 });
    for (const path of ["/onboarding", "/profile"]) {
      await page.goto(path);
      await expect(page.getByLabel("Casual", { exact: true })).toBeVisible();
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    }
  }
});
