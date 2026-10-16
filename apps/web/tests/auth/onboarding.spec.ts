import { expect, test } from "@playwright/test";
import { mockProfileBackend } from "../fixtures/profile-backend";

test("optional onboarding persists No preference separately from unanswered and completes explicitly", async ({ page }) => {
  const backend = await mockProfileBackend(page);
  await page.goto("/onboarding");
  const styles = page.getByRole("group", { name: "Clothing styles (optional)", exact: true });
  await styles.getByLabel("No preference", { exact: true }).check();
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(page.getByRole("heading", { name: "What feels comfortable", exact: true })).toBeFocused();
  await page.reload();
  await expect(page.getByRole("heading", { name: "What feels comfortable", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(page.locator("dl > div").filter({ has: page.getByText("Clothing styles", { exact: true }) }).locator("dd")).toHaveText("No preference");
  await expect(page.locator("dl > div").filter({ has: page.getByText("Preferred colors", { exact: true }) }).locator("dd")).toHaveText("Unanswered");
  await page.getByRole("button", { name: "Finish onboarding", exact: true }).click();
  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByRole("link", { name: "Edit preferences", exact: true })).toBeVisible();
  expect(backend.profile.onboardingStatus).toBe("COMPLETED");
  expect(backend.profile.preferences.styles).toEqual([]);
  expect(backend.profile.preferences.preferredColors).toBeNull();
  expect(backend.writes.map((write) => write.intent)).toEqual(["SAVE", "SAVE", "SAVE", "COMPLETE"]);
});

test("Back preserves local choices and review allows editing explicit coverage strength", async ({ page }) => {
  await mockProfileBackend(page);
  await page.goto("/onboarding");
  await page.getByLabel("Casual", { exact: true }).check();
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await page.getByLabel("Covered shoulders", { exact: true }).check();
  await page.getByLabel("How important is covered shoulders?", { exact: true }).selectOption("REQUIRED");
  await page.getByRole("button", { name: "Back", exact: true }).click();
  await expect(page.getByLabel("Casual", { exact: true })).toBeChecked();
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(page.getByLabel("How important is covered shoulders?", { exact: true })).toHaveValue("REQUIRED");
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(page.getByText("Covered shoulders (Required)", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Edit clothing styles", exact: true }).click();
  const styles = page.getByRole("group", { name: "Clothing styles (optional)", exact: true });
  await styles.getByRole("button", { name: "Clear choices", exact: true }).click();
  await expect(styles.getByText("Unanswered", { exact: true })).toBeVisible();
});

test("contradictory colors block saving and focus the validation summary", async ({ page }) => {
  const backend = await mockProfileBackend(page);
  await page.goto("/onboarding");
  await page.getByRole("group", { name: "Preferred colors (optional)", exact: true }).getByLabel("Navy", { exact: true }).check();
  await page.getByRole("group", { name: "Disliked colors (optional)", exact: true }).getByLabel("Navy", { exact: true }).check();
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(page.getByText("A color cannot be both preferred and disliked", { exact: true })).toBeVisible();
  await expect(page.getByRole("main").getByRole("alert")).toBeFocused();
  expect(backend.writes).toHaveLength(0);
});

test("mobile keyboard choices can be saved, skipped, and resumed without overflow", async ({ page }) => {
  const backend = await mockProfileBackend(page);
  await page.emulateMedia({ reducedMotion: "reduce" });
  await page.setViewportSize({ width: 360, height: 800 });
  await page.goto("/onboarding");
  await page.getByLabel("Casual", { exact: true }).focus(); await page.keyboard.press("Space");
  await page.getByRole("button", { name: "Save and exit", exact: true }).click();
  await expect(page).toHaveURL(/\/dashboard$/);
  await page.getByRole("link", { name: "Resume setup", exact: true }).click();
  await expect(page.getByLabel("Casual", { exact: true })).toBeChecked();
  await page.getByRole("button", { name: "Skip onboarding", exact: true }).click();
  await expect(page).toHaveURL(/\/dashboard$/);
  expect(backend.profile.onboardingStatus).toBe("SKIPPED");
  expect(backend.profile.preferences.styles).toEqual(["CASUAL"]);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
});
