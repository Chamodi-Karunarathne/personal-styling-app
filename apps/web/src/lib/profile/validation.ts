import { GROUP_KEYS, type PreferenceCatalog, type Preferences } from "./types";

export function validatePreferences(preferences: Preferences, catalog: PreferenceCatalog): Record<string, string> {
  const errors: Record<string, string> = {};
  for (const key of GROUP_KEYS) {
    const choices = preferences[key];
    if (choices === null) continue;
    const group = catalog.groups.find((entry) => entry.key === key);
    const codes = choices.map((choice) => typeof choice === "string" ? choice : choice.code);
    if (!group || codes.some((code) => !group.options.some((option) => option.code === code))) errors[key] = "Choose a supported option";
    else if (new Set(codes).size !== codes.length) errors[key] = "Choose each option only once";
  }
  if (preferences.coverage?.some((choice) => choice.strength !== "PREFERENCE" && choice.strength !== "REQUIRED")) errors.coverage = "Choose Preferred or Required";
  if (preferences.preferredColors?.some((code) => preferences.dislikedColors?.includes(code))) {
    errors.dislikedColors = "A color cannot be both preferred and disliked";
  }
  return errors;
}
