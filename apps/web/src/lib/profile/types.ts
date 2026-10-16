export const GROUP_KEYS = ["styles", "preferredColors", "dislikedColors", "fits", "footwear", "comfort", "coverage", "commonOccasions", "activities", "transportModes"] as const;
export type PreferenceKey = typeof GROUP_KEYS[number];
export type Strength = "PREFERENCE" | "REQUIRED";
export type CoverageChoice = { code: string; strength: Strength };
export type Preferences = { [K in Exclude<PreferenceKey, "coverage">]: string[] | null } & { coverage: CoverageChoice[] | null };
export type PreferenceGroup = { key: PreferenceKey; label: string; allowsRequired: boolean; options: { code: string; label: string; swatch?: string }[] };
export type PreferenceCatalog = { groups: PreferenceGroup[] };
export type StepCode = "STYLE" | "COMFORT" | "LIFESTYLE" | "REVIEW";
export type OnboardingStatus = "NOT_STARTED" | "IN_PROGRESS" | "SKIPPED" | "COMPLETED";
export type ProfileSnapshot = { version: number | null; onboardingStatus: OnboardingStatus; lastVisitedStep: StepCode; preferences: Preferences; completedAt: string | null; updatedAt: string | null };
export type ProfileWrite = { version: number | null; intent: "SAVE" | "COMPLETE" | "SKIP"; lastVisitedStep: StepCode; preferences: Preferences };

export function emptyPreferences(): Preferences {
  return { styles: null, preferredColors: null, dislikedColors: null, fits: null, footwear: null, comfort: null, coverage: null, commonOccasions: null, activities: null, transportModes: null };
}

export function clonePreferences(preferences: Preferences): Preferences {
  return structuredClone(preferences);
}

export function emptyProfile(): ProfileSnapshot {
  return { version: null, onboardingStatus: "NOT_STARTED", lastVisitedStep: "STYLE", preferences: emptyPreferences(), completedAt: null, updatedAt: null };
}

export const STEPS: { code: StepCode; title: string; description: string; groups: PreferenceKey[] }[] = [
  { code: "STYLE", title: "Your style & colors", description: "Choose the looks and colors you enjoy. Every answer is optional.", groups: ["styles", "preferredColors", "dislikedColors"] },
  { code: "COMFORT", title: "What feels comfortable", description: "Tell us what you like wearing, with optional coverage choices.", groups: ["fits", "footwear", "comfort", "coverage"] },
  { code: "LIFESTYLE", title: "Your everyday life", description: "Add typical occasions, activities, and transport as future defaults.", groups: ["commonOccasions", "activities", "transportModes"] },
  { code: "REVIEW", title: "Review your preferences", description: "Unanswered is different from No preference. You can finish without selecting anything.", groups: [] },
];
