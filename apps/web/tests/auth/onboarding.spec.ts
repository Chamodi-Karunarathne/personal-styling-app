import { expect, test, type Page } from "@playwright/test";

async function authenticated(page: Page) {
  await page.route("http://localhost:8080/api/auth/me", (route) => route.fulfill({
    status: 200,
    headers: { "Access-Control-Allow-Origin": `http://localhost:${process.env.PLAYWRIGHT_PORT ?? "3000"}`, "Access-Control-Allow-Credentials": "true" },
    json: { id: 7, fullName: "Example User", email: "user@example.com" },
  }));
}

test.beforeEach(async ({ page }) => { await authenticated(page); });

test("optional onboarding distinguishes unanswered and No preference without pretending to persist", async ({ page }) => {
  const writes: string[] = [];
  page.on("request", (request) => { if (["POST", "PUT", "PATCH"].includes(request.method())) writes.push(request.url()); });
  await page.goto("/onboarding");
  await expect(page.getByRole("heading", { name: "Your style & colors", exact: true })).toBeVisible();
  const styles = page.getByRole("group", { name: "Clothing styles (optional)", exact: true });
  await styles.getByLabel("No preference", { exact: true }).check();
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(page.getByRole("heading", { name: "What feels comfortable", exact: true })).toBeFocused();
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Review your preferences", exact: true })).toBeVisible();
  await expect(page.locator("dl > div").filter({ has: page.getByText("Clothing styles", { exact: true }) }).locator("dd")).toHaveText("No preference");
  await expect(page.locator("dl > div").filter({ has: page.getByText("Preferred colors", { exact: true }) }).locator("dd")).toHaveText("Unanswered");
  await page.getByRole("button", { name: "Finish onboarding", exact: true }).click();
  await expect(page.getByRole("status")).toContainText("have not been saved to the server");
  expect(writes).toEqual([]);
});

test("back preserves selections, review supports editing, and coverage strength is explicit", async ({ page }) => {
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
  await expect(page.getByLabel("Casual", { exact: true })).not.toBeChecked();
  await expect(styles.getByText("Unanswered", { exact: true })).toBeVisible();
});

test("contradictory colors block advancing and expose a useful validation message", async ({ page }) => {
  await page.goto("/onboarding");
  await page.getByRole("group", { name: "Preferred colors (optional)", exact: true }).getByLabel("Navy", { exact: true }).check();
  await page.getByRole("group", { name: "Disliked colors (optional)", exact: true }).getByLabel("Navy", { exact: true }).check();
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(page.getByText("A color cannot be both preferred and disliked", { exact: true })).toBeVisible();
  await expect(page.getByRole("main").getByRole("alert")).toContainText("Please check your choices");
  await expect(page.getByRole("main").getByRole("alert")).toBeFocused();
  await expect(page.getByRole("heading", { name: "Your style & colors", exact: true })).toBeVisible();
});

test("save and skip previews remain honest and layout fits small screens with reduced motion", async ({ page }) => {
  await page.emulateMedia({ reducedMotion: "reduce" });
  await page.setViewportSize({ width: 360, height: 800 });
  await page.goto("/onboarding");
  await page.getByRole("button", { name: "Save and exit", exact: true }).click();
  await expect(page.getByRole("status")).toContainText("have not been saved to the server");
  await page.getByRole("button", { name: "Skip onboarding", exact: true }).click();
  await expect(page).toHaveURL(/\/onboarding$/);
  await expect(page.getByRole("button", { name: "Next", exact: true })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  await page.getByLabel("Casual", { exact: true }).focus();
  await page.keyboard.press("Space");
  await expect(page.getByLabel("Casual", { exact: true })).toBeChecked();
});
