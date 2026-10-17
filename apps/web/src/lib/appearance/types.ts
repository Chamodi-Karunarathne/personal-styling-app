export const APPEARANCE_KEYS = ["heightCm", "garmentSizeNote", "bodyProportions", "skinTone", "skinUndertone", "paletteSuggestions", "hairColor", "hairTexture", "hairLength", "faceShape"] as const;
export const APPEARANCE_CHOICES = ["bodyProportions", "skinTone", "skinUndertone", "hairColor", "hairTexture", "hairLength", "faceShape"] as const;
export type AppearanceKey = typeof APPEARANCE_KEYS[number];
export type AppearanceChoice = typeof APPEARANCE_CHOICES[number];
export type AppearanceDetails = { heightCm: number | null; garmentSizeNote: string | null; paletteSuggestions: boolean | null } & { [K in AppearanceChoice]: string | null };
export type AppearanceOptions = { [K in AppearanceChoice]: { code: string; label: string }[] };
export type AppearanceSnapshot = { version: string | null; details: AppearanceDetails; updatedAt: string | null; options: AppearanceOptions };
export type AppearanceWrite = { version: string | null; details: AppearanceDetails };
export function emptyAppearance(): AppearanceDetails {
  return { heightCm: null, garmentSizeNote: null, bodyProportions: null, skinTone: null, skinUndertone: null, paletteSuggestions: null, hairColor: null, hairTexture: null, hairLength: null, faceShape: null };
}
export function normalizeAppearance(details: AppearanceDetails): AppearanceDetails {
  return { ...details, garmentSizeNote: details.garmentSizeNote?.trim() || null };
}
export function sameAppearance(first: AppearanceDetails, second: AppearanceDetails): boolean {
  const a = normalizeAppearance(first); const b = normalizeAppearance(second);
  return APPEARANCE_KEYS.every((key) => a[key] === b[key]);
}
export function validateAppearance(details: AppearanceDetails, options: AppearanceOptions): Record<string, string> {
  const errors: Record<string, string> = {};
  if (details.heightCm !== null && (!Number.isInteger(details.heightCm) || details.heightCm < 50 || details.heightCm > 260)) errors.heightCm = "Use whole centimeters between 50 and 260";
  if (details.garmentSizeNote !== null && details.garmentSizeNote.length > 60) errors.garmentSizeNote = "Sizing note must be at most 60 characters";
  for (const key of APPEARANCE_CHOICES) if (details[key] !== null && !options[key].some((option) => option.code === details[key])) errors[key] = "Choose a supported option";
  return errors;
}
