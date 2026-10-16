import { test, expect } from "@playwright/test";
import { INITIAL_CATALOG } from "../../src/lib/profile/catalog";
import { clonePreferences, emptyPreferences, emptyProfile } from "../../src/lib/profile/types";
import { validatePreferences } from "../../src/lib/profile/validation";

test("unanswered and explicit No preference remain different without mandatory personal answers", () => {
  const preferences = emptyPreferences();
  expect(validatePreferences(preferences, INITIAL_CATALOG)).toEqual({});
  preferences.styles = [];
  expect(preferences.preferredColors).toBeNull();
  expect(clonePreferences(preferences).styles).toEqual([]);
  expect(emptyProfile().onboardingStatus).toBe("NOT_STARTED");
});

test("known choices accept coverage strength without deriving restrictions from lifestyle", () => {
  const preferences = emptyPreferences();
  preferences.styles = ["CASUAL", "MINIMAL"];
  preferences.coverage = [{ code: "COVER_SHOULDERS", strength: "REQUIRED" }];
  preferences.transportModes = ["PUBLIC_TRANSPORT"];
  const original = clonePreferences(preferences);
  expect(validatePreferences(preferences, INITIAL_CATALOG)).toEqual({});
  expect(preferences).toEqual(original);
});

test("unknown duplicate and contradictory choices are rejected without modifying the draft", () => {
  const preferences = emptyPreferences();
  preferences.styles = ["UNKNOWN"];
  preferences.fits = ["RELAXED", "RELAXED"];
  preferences.preferredColors = ["NAVY"];
  preferences.dislikedColors = ["NAVY"];
  expect(validatePreferences(preferences, INITIAL_CATALOG)).toEqual({ styles: "Choose a supported option", fits: "Choose each option only once", dislikedColors: "A color cannot be both preferred and disliked" });
  expect(preferences.dislikedColors).toEqual(["NAVY"]);
});
