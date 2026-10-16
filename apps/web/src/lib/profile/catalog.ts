import type { PreferenceCatalog, PreferenceGroup } from "./types";

const options = (values: [string, string][]) => values.map(([code, label]) => ({ code, label }));
const colors = [
  ["BLACK", "Black", "#262323"], ["WHITE", "White", "#ffffff"], ["GREY", "Grey", "#858282"], ["BEIGE", "Beige", "#d8c5ac"],
  ["BROWN", "Brown", "#77533d"], ["NAVY", "Navy", "#26354e"], ["BLUE", "Blue", "#527fb5"], ["GREEN", "Green", "#59755a"],
  ["RED", "Red", "#a73742"], ["PINK", "Pink", "#d49aa9"], ["PURPLE", "Purple", "#8b689b"], ["YELLOW", "Yellow", "#e5c563"], ["ORANGE", "Orange", "#cc8351"],
].map(([code, label, swatch]) => ({ code, label, swatch }));

// Preview/fixture vocabulary. Integrated screens use the authenticated server catalog.
export const INITIAL_CATALOG: PreferenceCatalog = { groups: [
  { key: "styles", label: "Clothing styles", allowsRequired: false, options: options([["CASUAL", "Casual"], ["MINIMAL", "Minimal"], ["SMART_CASUAL", "Smart casual"], ["FORMAL", "Formal"], ["SPORTY", "Sporty"], ["STREETWEAR", "Streetwear"], ["VINTAGE", "Vintage"]]) },
  { key: "preferredColors", label: "Preferred colors", allowsRequired: false, options: colors },
  { key: "dislikedColors", label: "Disliked colors", allowsRequired: false, options: colors },
  { key: "fits", label: "Fit preferences", allowsRequired: false, options: options([["RELAXED", "Relaxed"], ["REGULAR", "Regular"], ["CLOSE_FITTING", "Close-fitting"]]) },
  { key: "footwear", label: "Footwear preferences", allowsRequired: false, options: options([["TRAINERS", "Trainers"], ["FLATS", "Flats"], ["SANDALS", "Sandals"], ["BOOTS", "Boots"], ["HEELS", "Heels"]]) },
  { key: "comfort", label: "Comfort priorities", allowsRequired: false, options: options([["BREATHABLE", "Lightweight / breathable"], ["EASY_MOVEMENT", "Easy movement"], ["SOFT_FABRICS", "Soft fabrics"]]) },
  { key: "coverage", label: "Coverage preferences (optional)", allowsRequired: true, options: options([["COVER_SHOULDERS", "Covered shoulders"], ["COVER_KNEES", "Covered knees"], ["LONGER_SLEEVES", "Longer sleeves"]]) },
  { key: "commonOccasions", label: "Typical occasions", allowsRequired: false, options: options([["WORK_STUDY", "Work / study"], ["CASUAL_OUTINGS", "Casual outings"], ["FORMAL_EVENTS", "Formal events"], ["ACTIVE_OUTDOOR", "Active / outdoor activities"]]) },
  { key: "activities", label: "Typical activities", allowsRequired: false, options: options([["MOSTLY_SEATED", "Mostly seated"], ["WALKING", "Walking"], ["OUTDOOR_TIME", "Outdoor time"]]) },
  { key: "transportModes", label: "Common transportation", allowsRequired: false, options: options([["WALKING", "Walking"], ["PUBLIC_TRANSPORT", "Public transport"], ["CAR", "Car"], ["CYCLING", "Cycling"]]) },
] satisfies PreferenceGroup[] };
