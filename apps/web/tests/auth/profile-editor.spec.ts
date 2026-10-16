import { expect, test, type Page } from "@playwright/test";

async function authenticated(page: Page) {
  await page.route("http://localhost:8080/api/auth/me", (route) => route.fulfill({
    status: 200,
    headers: { "Access-Control-Allow-Origin": `http://localhost:${process.env.PLAYWRIGHT_PORT ?? "3000"}`, "Access-Control-Allow-Credentials": "true" },
    json: { id: 7, fullName: "Example User", email: "user@example.com" },
  }));
}

test.beforeEach(async ({ page }) => { await authenticated(page); });

test("profile editor reuses choices and displays account identity as read-only", async ({ page }) => {
  const writes: string[] = [];
  page.on("request", (request) => { if (["POST", "PUT", "PATCH"].includes(request.method())) writes.push(request.url()); });
  await page.goto("/profile");
  await expect(page.getByRole("heading", { name: "Your styling profile", exact: true })).toBeVisible();
  await expect(page.getByText("Example User", { exact: true })).toBeVisible();
  await expect(page.getByText("user@example.com", { exact: true })).toBeVisible();
  await expect(page.getByRole("textbox")).toHaveCount(0);
  await page.getByLabel("Casual", { exact: true }).check();
  await page.getByRole("group", { name: "Preferred colors (optional)", exact: true }).getByLabel("No preference", { exact: true }).check();
  await page.getByRole("button", { name: "Save preferences", exact: true }).click();
  await expect(page.getByRole("status")).toContainText("have not been saved to the server");
  await expect(page.getByLabel("Casual", { exact: true })).toBeChecked();
  expect(writes).toEqual([]);
});

test("Cancel discards preview edits and clearing a group restores unanswered", async ({ page }) => {
  await page.goto("/profile");
  const styles = page.getByRole("group", { name: "Clothing styles (optional)", exact: true });
  await styles.getByLabel("Casual", { exact: true }).check();
  await styles.getByLabel("Casual", { exact: true }).uncheck();
  await expect(styles.getByLabel("No preference", { exact: true })).not.toBeChecked();
  await expect(styles.getByText("Unanswered", { exact: true })).toBeVisible();
  await styles.getByLabel("No preference", { exact: true }).check();
  await expect(styles.getByText("Unanswered", { exact: true })).toHaveCount(0);
  await styles.getByRole("button", { name: "Clear choices", exact: true }).click();
  await expect(styles.getByText("Unanswered", { exact: true })).toBeVisible();
  await page.getByLabel("Casual", { exact: true }).check();
  await page.getByRole("button", { name: "Save preferences", exact: true }).click();
  page.once("dialog", (dialog) => dialog.accept());
  await page.getByRole("button", { name: "Cancel", exact: true }).click();
  await expect(page.getByLabel("Casual", { exact: true })).not.toBeChecked();
  await expect(styles.getByText("Unanswered", { exact: true })).toBeVisible();
  await expect(page.getByRole("status")).toHaveText("Your unsaved changes have been discarded.");
});

test("profile validation preserves edits and keeps small-screen fields usable", async ({ page }) => {
  await page.setViewportSize({ width: 360, height: 800 });
  await page.goto("/profile");
  await page.getByRole("group", { name: "Preferred colors (optional)", exact: true }).getByLabel("Navy", { exact: true }).check();
  await page.getByRole("group", { name: "Disliked colors (optional)", exact: true }).getByLabel("Navy", { exact: true }).check();
  await page.getByRole("button", { name: "Save preferences", exact: true }).click();
  await expect(page.locator("main").getByRole("alert")).toContainText("Please check your choices");
  await expect(page.getByText("A color cannot be both preferred and disliked", { exact: true })).toBeVisible();
  await expect(page.getByRole("group", { name: "Preferred colors (optional)", exact: true }).getByLabel("Navy", { exact: true })).toBeChecked();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
});
