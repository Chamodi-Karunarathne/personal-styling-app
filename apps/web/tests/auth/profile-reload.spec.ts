import { expect, test } from "@playwright/test";
import { mockProfileBackend } from "../fixtures/profile-backend";

test("styling conflict reload waits behind a personal save without aborting or remounting it", async ({ page }) => {
  const backend = await mockProfileBackend(page); await page.goto("/profile");
  await page.getByLabel("Casual", { exact: true }).check(); backend.failWrite = { status: 409 };
  await page.getByRole("button", { name: "Save preferences", exact: true }).click();
  await expect(page.getByRole("button", { name: "Reload saved profile", exact: true })).toBeVisible();
  let release!: () => void; const gate = new Promise<void>((resolve) => { release = resolve; });
  let dispatch!: () => void; const started = new Promise<void>((resolve) => { dispatch = resolve; });
  backend.before = async (path, method) => { if (path === "/api/profile/appearance" && method === "PUT") { dispatch(); await gate; } };
  const personal = page.getByRole("region", { name: "Personal Details & Fit", exact: true });
  try {
    await personal.getByLabel(/^Height \(cm\)/).fill("172");
    await personal.getByRole("button", { name: "Save personal details", exact: true }).click(); await started;
    page.once("dialog", (dialog) => dialog.accept()); await page.getByRole("button", { name: "Reload saved profile", exact: true }).click();
    await expect(personal.getByRole("button", { name: "Saving", exact: false })).toBeDisabled();
    await expect(personal.getByLabel(/^Height \(cm\)/)).toHaveValue("172"); release();
    await expect(personal.getByRole("status")).toContainText("have been saved");
    await expect(page.getByText("Saved styling preferences reloaded. Personal details are unchanged.", { exact: true })).toBeVisible();
    await expect(personal.getByLabel(/^Height \(cm\)/)).toHaveValue("172");
    await expect(page.getByLabel("Casual", { exact: true })).not.toBeChecked();
    expect(backend.appearanceWrites).toHaveLength(1); expect(backend.appearance.details.heightCm).toBe(172);
  } finally { release(); }
});

test("styling reload preserves unsaved personal edits and remains recoverable after a failed read", async ({ page }) => {
  const backend = await mockProfileBackend(page); await page.goto("/profile");
  await page.getByLabel("Casual", { exact: true }).check(); await page.getByLabel(/^Height \(cm\)/).fill("172");
  backend.failWrite = { status: 409 }; await page.getByRole("button", { name: "Save preferences", exact: true }).click();
  backend.failRead = true; page.once("dialog", (dialog) => dialog.accept());
  await page.getByRole("button", { name: "Reload saved profile", exact: true }).click();
  await expect(page.locator("main").getByRole("alert")).toContainText("Something went wrong");
  await expect(page.getByLabel(/^Height \(cm\)/)).toHaveValue("172");
  await expect(page.getByLabel("Casual", { exact: true })).toBeChecked();
  backend.failRead = false; page.once("dialog", (dialog) => dialog.accept());
  await page.getByRole("button", { name: "Reload saved profile", exact: true }).click();
  await expect(page.getByText("Saved styling preferences reloaded. Personal details are unchanged.", { exact: true })).toBeVisible();
  await expect(page.getByLabel(/^Height \(cm\)/)).toHaveValue("172");
  expect(backend.appearanceWrites).toHaveLength(0);
});
