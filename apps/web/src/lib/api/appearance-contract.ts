import { APPEARANCE_CHOICES, emptyAppearance, validateAppearance, type AppearanceOptions, type AppearanceSnapshot } from "../appearance/types";
import { record, unexpected } from "./session-client";

export function parseAppearance(value: unknown, uncertain = false): AppearanceSnapshot {
  const fail = () => { throw unexpected(uncertain); };
  if (!record(value) || !record(value.details) || !record(value.options)) return fail();
  if (!(value.version === null || (typeof value.version === "string" && /^[\da-f]{8}-(?:[\da-f]{4}-){3}[\da-f]{12}$/i.test(value.version)))
      || !(value.updatedAt === null || (typeof value.updatedAt === "string" && Number.isFinite(Date.parse(value.updatedAt))))
      || (value.version === null) !== (value.updatedAt === null)) return fail();
  const options = {} as AppearanceOptions;
  for (const key of APPEARANCE_CHOICES) {
    const values = value.options[key];
    if (!Array.isArray(values) || !values.length || !values.every((option: unknown) => record(option) && typeof option.code === "string" && typeof option.label === "string")) return fail();
    options[key] = values.map((option) => ({ code: option.code, label: option.label }));
    if (new Set(options[key].map((option) => option.code)).size !== options[key].length) return fail();
  }
  const details = emptyAppearance();
  for (const key of APPEARANCE_CHOICES) {
    const choice = value.details[key]; if (!(choice === null || typeof choice === "string")) return fail(); details[key] = choice;
  }
  if (!(value.details.heightCm === null || typeof value.details.heightCm === "number")
      || !(value.details.garmentSizeNote === null || typeof value.details.garmentSizeNote === "string")
      || !(value.details.paletteSuggestions === null || typeof value.details.paletteSuggestions === "boolean")) return fail();
  details.heightCm = value.details.heightCm; details.garmentSizeNote = value.details.garmentSizeNote; details.paletteSuggestions = value.details.paletteSuggestions;
  if (Object.keys(validateAppearance(details, options)).length) return fail();
  return { version: value.version as string | null, details, updatedAt: value.updatedAt as string | null, options };
}
