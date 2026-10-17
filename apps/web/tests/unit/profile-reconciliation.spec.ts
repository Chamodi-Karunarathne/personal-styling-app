import { expect, test } from "@playwright/test";
import { confirmsWrite } from "../../src/lib/profile/reconciliation";
import { emptyProfile, type ProfileSnapshot, type ProfileWrite } from "../../src/lib/profile/types";

const input = (): ProfileWrite => ({ version: 1, intent: "SAVE", lastVisitedStep: "COMFORT", preferences: emptyProfile().preferences });
const saved = (): ProfileSnapshot => ({ ...emptyProfile(), version: 2, onboardingStatus: "IN_PROGRESS", lastVisitedStep: "COMFORT", updatedAt: "2026-10-17T00:00:00Z" });

test("uncertain saves require a newer version, intended status, resume step, and complete matching snapshot", () => {
  expect(confirmsWrite(saved(), input(), "IN_PROGRESS")).toBe(true);
  expect(confirmsWrite({ ...saved(), version: 1 }, input(), "IN_PROGRESS")).toBe(false);
  expect(confirmsWrite({ ...saved(), lastVisitedStep: "STYLE" }, input(), "IN_PROGRESS")).toBe(false);
  expect(confirmsWrite({ ...saved(), onboardingStatus: "SKIPPED" }, input(), "IN_PROGRESS")).toBe(false);
  const remote = saved(); remote.preferences.styles = [];
  expect(confirmsWrite(remote, input(), "IN_PROGRESS")).toBe(false);
});
test("reconciliation ignores ordering but preserves unanswered, No preference, and coverage strength", () => {
  const request = input(); const remote = saved();
  request.preferences.styles = ["CASUAL", "MINIMAL"]; remote.preferences.styles = ["MINIMAL", "CASUAL"];
  expect(confirmsWrite(remote, request, "IN_PROGRESS")).toBe(true);
  request.preferences.coverage = [{ code: "COVER_KNEES", strength: "REQUIRED" }];
  remote.preferences.coverage = [{ code: "COVER_KNEES", strength: "PREFERENCE" }];
  expect(confirmsWrite(remote, request, "IN_PROGRESS")).toBe(false);
});
test("Skip, Complete, and completed profile edits retain their explicit status semantics", () => {
  expect(confirmsWrite({ ...saved(), onboardingStatus: "COMPLETED" }, { ...input(), intent: "COMPLETE" }, "IN_PROGRESS")).toBe(true);
  expect(confirmsWrite({ ...saved(), onboardingStatus: "SKIPPED" }, { ...input(), intent: "SKIP" }, "IN_PROGRESS")).toBe(true);
  expect(confirmsWrite({ ...saved(), onboardingStatus: "COMPLETED" }, input(), "COMPLETED")).toBe(true);
});

test("new appearance resume metadata participates in uncertain-write reconciliation", () => {
  const request = { ...input(), lastVisitedStep: "REVIEW" as const, appearanceStepPending: true };
  const remote = { ...saved(), lastVisitedStep: "REVIEW" as const, appearanceStepPending: false };
  expect(confirmsWrite(remote, request, "IN_PROGRESS")).toBe(false);
  expect(confirmsWrite({ ...remote, appearanceStepPending: true }, request, "IN_PROGRESS")).toBe(true);
});
