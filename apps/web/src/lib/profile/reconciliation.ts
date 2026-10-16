import { GROUP_KEYS, type Preferences, type ProfileSnapshot, type ProfileWrite } from "./types";

function canonical(preferences: Preferences): string {
  return JSON.stringify(GROUP_KEYS.map((key) => {
    const values = preferences[key];
    if (values === null) return null;
    return key === "coverage"
      ? preferences.coverage!.map((choice) => `${choice.code}:${choice.strength}`).sort()
      : [...values as string[]].sort();
  }));
}

/** Confirm an uncertain write with a read; never resubmit the PUT automatically. */
export function confirmsWrite(saved: ProfileSnapshot, input: ProfileWrite, previousStatus: ProfileSnapshot["onboardingStatus"]): boolean {
  const status = input.intent === "COMPLETE" ? "COMPLETED" : input.intent === "SKIP" ? "SKIPPED"
    : previousStatus === "COMPLETED" ? "COMPLETED" : "IN_PROGRESS";
  return saved.version !== null && (input.version === null || saved.version > input.version)
    && saved.onboardingStatus === status && saved.lastVisitedStep === input.lastVisitedStep
    && canonical(saved.preferences) === canonical(input.preferences);
}
