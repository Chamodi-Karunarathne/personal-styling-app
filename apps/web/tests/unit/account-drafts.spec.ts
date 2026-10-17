import { expect, test } from "@playwright/test";
import { AccountDrafts } from "../../src/lib/account-drafts";

test("drafts survive same-account verification and are isolated and discarded on account changes", () => {
  const drafts = new AccountDrafts(); drafts.verify(7);
  const input = { styles: ["CASUAL"] }; drafts.remember(7, "profile", input); input.styles.push("FORMAL");
  drafts.verify(7); expect(drafts.read(7, "profile")).toEqual({ styles: ["CASUAL"] });
  expect(drafts.read(8, "profile")).toBeUndefined(); drafts.verify(8); drafts.remember(7, "profile", input);
  expect(drafts.read(8, "profile")).toBeUndefined(); drafts.verify(7); expect(drafts.read(7, "profile")).toBeUndefined();
});

test("confirmed discard and logout erase memory, and reads cannot mutate cached snapshots", () => {
  const drafts = new AccountDrafts(); drafts.verify(7); drafts.remember(7, "personal", { height: 172 });
  drafts.read<{ height: number }>(7, "personal")!.height = 180;
  expect(drafts.read(7, "personal")).toEqual({ height: 172 }); drafts.remember(7, "personal", null);
  expect(drafts.read(7, "personal")).toBeUndefined(); drafts.remember(7, "personal", { height: 172 });
  drafts.clear(); drafts.verify(7); expect(drafts.read(7, "personal")).toBeUndefined();
});

test("removing personal draft fields preserves styling state and ignores another account", () => {
  const drafts = new AccountDrafts(); drafts.verify(7);
  drafts.remember(7, "onboarding", { preferences: { styles: ["CASUAL"] }, appearanceDraft: { heightCm: 172 } });
  const clear = (value: { preferences: object; appearanceDraft: object | null }) => ({ ...value, appearanceDraft: null });
  drafts.update(8, "onboarding", clear);
  expect(drafts.read(7, "onboarding")).toHaveProperty("appearanceDraft.heightCm", 172);
  drafts.update(7, "onboarding", clear);
  expect(drafts.read(7, "onboarding")).toEqual({ preferences: { styles: ["CASUAL"] }, appearanceDraft: null });
});
