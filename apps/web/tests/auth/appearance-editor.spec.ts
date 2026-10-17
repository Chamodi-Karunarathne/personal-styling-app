import { expect, test } from "@playwright/test";
import { mockProfileBackend } from "../fixtures/profile-backend";

test("all optional personal fields save, refresh, clear, and delete independently", async ({ page }) => {
  const backend = await mockProfileBackend(page); const originalStyle = structuredClone(backend.profile);
  await page.goto("/profile"); const section = page.getByRole("region", { name: "Personal Details & Fit", exact: true });
  await expect(section.getByRole("button", { name: "Delete personal details", exact: true })).toBeDisabled();
  await section.getByLabel(/^Height \(cm\)/).fill("172");
  await section.getByLabel(/^Skin undertone/).selectOption("UNSURE");
  await section.getByLabel(/^Interested in future/).selectOption("false");
  await section.getByLabel(/^Self-described face shape/).selectOption("NOT_SPECIFIED");
  await section.getByRole("button", { name: "Save personal details", exact: true }).click();
  await expect(section.getByRole("status")).toContainText("have been saved"); await page.reload();
  await expect(section.getByLabel(/^Height \(cm\)/)).toHaveValue("172");
  await expect(section.getByLabel(/^Interested in future/)).toHaveValue("false");
  await section.getByLabel(/^Height \(cm\)/).fill(""); await section.getByRole("button", { name: "Save personal details", exact: true }).click();
  await expect.poll(() => backend.appearance.details.heightCm).toBeNull();
  page.once("dialog", (dialog) => dialog.accept()); await section.getByRole("button", { name: "Delete personal details", exact: true }).click();
  await expect(section.getByRole("status")).toContainText("Personal details deleted");
  expect(backend.appearance.version).toBeNull(); expect(backend.profile).toEqual(originalStyle);
  expect(backend.appearanceWrites).toHaveLength(2); expect(backend.appearanceDeletes).toHaveLength(1);
});
test("height validation is linked, keyboard controls work, and mobile has no overflow", async ({ page }) => {
  const backend = await mockProfileBackend(page); await page.setViewportSize({ width: 360, height: 850 });
  await page.goto("/profile"); const section = page.getByRole("region", { name: "Personal Details & Fit", exact: true });
  await section.getByLabel(/^Height \(cm\)/).fill("172.5"); await section.getByRole("button", { name: "Save personal details", exact: true }).click();
  await expect(section.getByLabel(/^Height \(cm\)/)).toHaveAttribute("aria-invalid", "true");
  await expect(section.getByRole("alert")).toBeFocused(); expect(backend.appearanceWrites).toHaveLength(0);
  await section.getByLabel(/^Height \(cm\)/).fill(""); await section.getByLabel(/^Hair color/).focus();
  await page.keyboard.press("ArrowDown"); await page.keyboard.press("Enter");
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await expect(section.getByLabel(/\b(?:weight|date of birth|age)\b/i)).toHaveCount(0);
});
test("loading errors and conflicts preserve styling and personal edits", async ({ page }) => {
  const backend = await mockProfileBackend(page); backend.failAppearanceRead = true;
  await page.goto("/profile"); const section = page.getByRole("region", { name: "Personal Details & Fit", exact: true });
  await expect(section.getByRole("alert")).toBeVisible(); backend.failAppearanceRead = false;
  await section.getByRole("button", { name: "Retry personal details", exact: true }).click();
  await section.getByLabel(/^Height \(cm\)/).fill("172"); backend.failAppearance = { status: 409 };
  await section.getByRole("button", { name: "Save personal details", exact: true }).click();
  await expect(section.getByRole("alert")).toContainText("personal details changed elsewhere");
  await expect(section.getByLabel(/^Height \(cm\)/)).toHaveValue("172");
  await expect(section.getByRole("button", { name: "Save personal details", exact: true })).toBeDisabled();
  page.once("dialog", (dialog) => dialog.accept()); await section.getByRole("button", { name: "Reload personal details", exact: true }).click();
  await expect(section.getByLabel(/^Height \(cm\)/)).toHaveValue(""); expect(backend.writes).toHaveLength(0);
});
test("delete requires confirmation and uncertain successful deletion is reconciled", async ({ page }) => {
  const backend = await mockProfileBackend(page); await page.goto("/profile"); const section = page.getByRole("region", { name: "Personal Details & Fit", exact: true });
  await section.getByRole("button", { name: "Save personal details", exact: true }).click();
  await expect(section.getByRole("status")).toContainText("have been saved");
  page.once("dialog", (dialog) => dialog.dismiss()); await section.getByRole("button", { name: "Delete personal details", exact: true }).click();
  expect(backend.appearanceDeletes).toHaveLength(0);
  backend.failAppearance = { loseResponse: true, commit: true }; page.once("dialog", (dialog) => dialog.accept());
  await section.getByRole("button", { name: "Delete personal details", exact: true }).click();
  await expect(section.getByRole("status")).toContainText("Personal details deleted"); expect(backend.appearanceDeletes).toHaveLength(1);
});
test("unsaved personal edits prompt before leaving and Cancel restores only personal details", async ({ page }) => {
  await mockProfileBackend(page); await page.goto("/profile"); const section = page.getByRole("region", { name: "Personal Details & Fit", exact: true });
  await page.getByLabel("Casual", { exact: true }).check(); await section.getByLabel(/^Height \(cm\)/).fill("172");
  page.once("dialog", (dialog) => dialog.dismiss()); await page.getByRole("link", { name: "Back to dashboard", exact: true }).click();
  await expect(page).toHaveURL(/\/profile$/); page.once("dialog", (dialog) => dialog.accept());
  await section.getByRole("button", { name: "Cancel personal details changes", exact: true }).click();
  await expect(section.getByLabel(/^Height \(cm\)/)).toHaveValue(""); await expect(page.getByLabel("Casual", { exact: true })).toBeChecked();
});
