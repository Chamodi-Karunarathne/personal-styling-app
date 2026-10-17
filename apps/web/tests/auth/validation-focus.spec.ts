import { expect, test } from "@playwright/test";
import { mockProfileBackend } from "../fixtures/profile-backend";

for (const form of ["styling", "personal", "onboarding"] as const) test(`${form} summary regains focus on repeated identical validation failures`, async ({ page }) => {
  const backend = await mockProfileBackend(page); await page.goto(form === "onboarding" ? "/onboarding" : "/profile");
  if (form === "personal") await page.getByLabel(/^Height \(cm\)/).fill("172.5");
  else {
    await page.getByRole("group", { name: "Preferred colors (optional)", exact: true }).getByLabel("Navy", { exact: true }).check();
    await page.getByRole("group", { name: "Disliked colors (optional)", exact: true }).getByLabel("Navy", { exact: true }).check();
  }
  const button = page.getByRole("button", { name: form === "personal" ? "Save personal details" : form === "styling" ? "Save preferences" : "Next", exact: true });
  const scope = form === "personal" ? page.getByRole("region", { name: "Personal Details & Fit", exact: true }) : page.locator("main");
  await button.click(); await expect(scope.getByRole("alert")).toBeFocused();
  await button.focus(); await expect(button).toBeFocused(); await page.keyboard.press("Enter");
  await expect(scope.getByRole("alert")).toBeFocused();
  expect(backend.writes).toHaveLength(0); expect(backend.appearanceWrites).toHaveLength(0);
});
