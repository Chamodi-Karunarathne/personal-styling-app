import { emptyAppearance, type AppearanceOptions, type AppearanceSnapshot } from "../../src/lib/appearance/types";
const choices = (...codes: string[]) => codes.map((code) => ({ code, label: code === "UNSURE" ? "Unsure" : code === "NOT_SPECIFIED" ? "Prefer not to specify" : code[0] + code.slice(1).toLowerCase().replaceAll("_", " ") }));
export const APPEARANCE_OPTIONS: AppearanceOptions = {
  bodyProportions: choices("BALANCED", "LONGER_TORSO", "LONGER_LEGS", "UNSURE"),
  skinTone: choices("VERY_LIGHT", "LIGHT", "MEDIUM", "DEEP", "VERY_DEEP", "UNSURE"),
  skinUndertone: choices("WARM", "COOL", "NEUTRAL", "UNSURE"),
  hairColor: choices("BLACK", "BROWN", "BLONDE", "RED", "GREY", "WHITE", "OTHER", "UNSURE"),
  hairTexture: choices("STRAIGHT", "WAVY", "CURLY", "COILY", "UNSURE"),
  hairLength: choices("SHAVED", "SHORT", "MEDIUM", "LONG", "UNSURE"),
  faceShape: choices("OVAL", "ROUND", "SQUARE", "OBLONG", "HEART", "DIAMOND", "UNSURE", "NOT_SPECIFIED"),
};
export function emptyAppearanceSnapshot(): AppearanceSnapshot { return { version: null, details: emptyAppearance(), updatedAt: null, options: structuredClone(APPEARANCE_OPTIONS) }; }
