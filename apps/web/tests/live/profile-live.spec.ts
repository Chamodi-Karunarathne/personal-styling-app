import { expect, test } from "@playwright/test";

test("real browser registers, restores drafts, completes, edits, and isolates another account", async ({ page, browser }) => {
  const email = `profile-browser-${crypto.randomUUID()}@example.com`;
  const password = "SyntheticBrowserTest123";
  const errors: string[] = []; page.on("pageerror", (error) => errors.push(error.message));
  await page.goto("/register");
  await page.getByLabel("Full name", { exact: true }).fill("Profile Browser Test");
  await page.getByLabel("Email address", { exact: true }).fill(email);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Create Account", exact: true }).click();
  await expect(page).toHaveURL(/\/login$/);
  await page.getByLabel("Email address", { exact: true }).fill(email);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Sign In", exact: true }).click();
  await expect(page).toHaveURL(/\/dashboard$/);
  await page.getByRole("link", { name: "Start setup", exact: true }).click();
  await page.getByLabel("Casual", { exact: true }).check();
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(page.getByRole("heading", { name: "What feels comfortable", exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByRole("heading", { name: "What feels comfortable", exact: true })).toBeVisible();
  await page.getByRole("group", { name: "Fit preferences (optional)", exact: true }).getByLabel("No preference", { exact: true }).check();
  await page.getByLabel("Covered knees", { exact: true }).check();
  await page.getByLabel("How important is covered knees?", { exact: true }).selectOption("REQUIRED");
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Your everyday life", exact: true })).toBeVisible();
  await page.getByLabel("Public transport", { exact: true }).check();
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(page.getByText("Covered knees (Required)", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Finish onboarding", exact: true }).click();
  await expect(page).toHaveURL(/\/dashboard$/);
  await page.getByRole("link", { name: "Edit preferences", exact: true }).click();
  await expect(page.getByLabel("Casual", { exact: true })).toBeChecked();
  await expect(page.getByRole("group", { name: "Fit preferences (optional)", exact: true }).getByLabel("No preference", { exact: true })).toBeChecked();
  await page.getByLabel("Minimal", { exact: true }).check();
  await page.getByRole("button", { name: "Save preferences", exact: true }).click();
  await expect(page.getByRole("status")).toContainText("have been saved");
  await page.reload();
  await expect(page.getByLabel("Minimal", { exact: true })).toBeChecked();
  expect(await page.evaluate(() => localStorage.length + sessionStorage.length)).toBe(0);
  const second = await browser.newContext();
  try {
    const api = second.request;
    const csrf = await (await api.get("http://localhost:8081/api/auth/csrf")).json();
    const secondEmail = `profile-second-${crypto.randomUUID()}@example.com`;
    expect((await api.post("http://localhost:8081/api/auth/register", { headers: { [csrf.headerName]: csrf.token },
      data: { fullName: "Second Browser Test", email: secondEmail, password } })).status()).toBe(201);
    expect((await api.post("http://localhost:8081/api/auth/login", { headers: { [csrf.headerName]: csrf.token }, data: { email: secondEmail, password } })).status()).toBe(200);
    const other = await second.newPage(); await other.goto("/profile");
    await expect(other.getByLabel("Casual", { exact: true })).not.toBeChecked();
    await expect(other.getByText("Second Browser Test", { exact: true })).toBeVisible();
  } finally { await second.close(); }
  await page.getByRole("link", { name: "Back to dashboard", exact: true }).click();
  await page.getByRole("button", { name: "Sign out", exact: true }).click();
  await expect(page).toHaveURL(/\/login$/);
  await page.goto("/profile"); await expect(page).toHaveURL(/\/login$/);
  expect(errors).toEqual([]);
});
