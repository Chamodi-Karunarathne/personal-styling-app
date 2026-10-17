export const GROUP_KEYS = ["styles", "preferredColors", "dislikedColors", "fits", "footwear", "comfort", "coverage", "commonOccasions", "activities", "transportModes"] as const;
export type PreferenceKey = typeof GROUP_KEYS[number];
export type Strength = "PREFERENCE" | "REQUIRED";
export type CoverageChoice = { code: string; strength: Strength };
export type Preferences = { [K in Exclude<PreferenceKey, "coverage">]: string[] | null } & { coverage: CoverageChoice[] | null };
export type PreferenceGroup = { key: PreferenceKey; label: string; allowsRequired: boolean; options: { code: string; label: string; swatch?: string }[] };
export type PreferenceCatalog = { groups: PreferenceGroup[] };
export type StepCode = "STYLE" | "COMFORT" | "LIFESTYLE" | "APPEARANCE" | "REVIEW";
export type StoredStepCode = Exclude<StepCode, "APPEARANCE">;
export type OnboardingStatus = "NOT_STARTED" | "IN_PROGRESS" | "SKIPPED" | "COMPLETED";
export type ProfileSnapshot = { version: number | null; onboardingStatus: OnboardingStatus; lastVisitedStep: StoredStepCode; preferences: Preferences; completedAt: string | null; updatedAt: string | null; appearanceStepPending?: boolean };
export type ProfileWrite = { version: number | null; intent: "SAVE" | "COMPLETE" | "SKIP"; lastVisitedStep: StoredStepCode; preferences: Preferences; appearanceStepPending?: boolean };

export function emptyPreferences(): Preferences {
  return { styles: null, preferredColors: null, dislikedColors: null, fits: null, footwear: null, comfort: null, coverage: null, commonOccasions: null, activities: null, transportModes: null };
}

export function clonePreferences(preferences: Preferences): Preferences {
  return structuredClone(preferences);
}

export function emptyProfile(): ProfileSnapshot {
  return { version: null, onboardingStatus: "NOT_STARTED", lastVisitedStep: "STYLE", preferences: emptyPreferences(), completedAt: null, updatedAt: null, appearanceStepPending: false };
}

export const STEPS: { code: StepCode; title: string; description: string; groups: PreferenceKey[] }[] = [
  { code: "STYLE", title: "Your style & colors", description: "Choose the looks and colors you enjoy. Every answer is optional.", groups: ["styles", "preferredColors", "dislikedColors"] },
  { code: "COMFORT", title: "What feels comfortable", description: "Tell us what you like wearing, with optional coverage choices.", groups: ["fits", "footwear", "comfort", "coverage"] },
  { code: "LIFESTYLE", title: "Your everyday life", description: "Add typical occasions, activities, and transport as future defaults.", groups: ["commonOccasions", "activities", "transportModes"] },
  { code: "APPEARANCE", title: "Personal details and fit", description: "Optional details for future personalization. Skip without changing your saved personal details.", groups: [] },
  { code: "REVIEW", title: "Review your preferences", description: "Unanswered is different from No preference. You can finish without selecting anything.", groups: [] },
];
