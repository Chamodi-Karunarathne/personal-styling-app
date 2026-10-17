"use client";

import type { AppearanceDetails, AppearanceKey, AppearanceOptions } from "@/lib/appearance/types";

export const APPEARANCE_LABELS: Record<AppearanceKey, string> = {
  heightCm: "Height (cm)", garmentSizeNote: "Usual garment size / sizing note", bodyProportions: "Self-described proportions",
  skinTone: "Skin tone", skinUndertone: "Skin undertone", paletteSuggestions: "Interested in future color palette suggestions?",
  hairColor: "Hair color", hairTexture: "Hair texture", hairLength: "Hair length", faceShape: "Self-described face shape",
};
const groups: { title: string; explanation: string; fields: AppearanceKey[] }[] = [
  { title: "Fit and sizing", explanation: "Optional context for future garment sizing and length suggestions. Your existing fit preferences stay separate.", fields: ["heightCm", "garmentSizeNote", "bodyProportions"] },
  { title: "Complexion and color", explanation: "Self-selected details only. They will never automatically rule out clothing colors. Palette suggestions are not available yet.", fields: ["skinTone", "skinUndertone", "paletteSuggestions"] },
  { title: "Hair characteristics", explanation: "Optional context for future styling and accessory ideas. No information is inferred from photos.", fields: ["hairColor", "hairTexture", "hairLength"] },
  { title: "Face shape and accessories", explanation: "Describe this only if you want to. Unsure and Prefer not to specify are equally valid choices.", fields: ["faceShape"] },
];
export function AppearanceFields({ details, options, onChange, errors = {}, disabled = false }: {
  details: AppearanceDetails; options: AppearanceOptions; onChange: (details: AppearanceDetails) => void; errors?: Record<string, string>; disabled?: boolean;
}) {
  const control = "h-12 w-full min-w-0 rounded-lg border border-input bg-background px-3 text-base focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";
  return <div className="space-y-8">{groups.map((group) => <fieldset key={group.title} disabled={disabled} className="min-w-0 space-y-4">
    <legend className="text-base font-semibold">{group.title}</legend>
    <p className="text-sm leading-relaxed text-muted-foreground">{group.explanation}</p>
    <div className="grid min-w-0 gap-5 sm:grid-cols-2">{group.fields.map((key) => {
      const id = `appearance-${key}`; const error = errors[key] ?? errors[`details.${key}`];
      const accessibility = { id, "aria-invalid": error ? true as const : undefined, "aria-describedby": `${id}-hint${error ? ` ${id}-error` : ""}`, className: control };
      return <div key={key} className="min-w-0 space-y-2">
        <label htmlFor={id} className="block text-sm font-medium">{APPEARANCE_LABELS[key]} <span className="font-normal text-muted-foreground">(optional)</span></label>
        {key === "heightCm" ? <input {...accessibility} type="number" min={50} max={260} step={1} inputMode="numeric" value={details.heightCm ?? ""} onChange={(event) => onChange({ ...details, heightCm: event.target.value === "" ? null : Number(event.target.value) })} />
          : key === "garmentSizeNote" ? <input {...accessibility} type="text" autoComplete="off" maxLength={60} value={details.garmentSizeNote ?? ""} placeholder="For example: M / UK 12" onChange={(event) => onChange({ ...details, garmentSizeNote: event.target.value || null })} />
          : key === "paletteSuggestions" ? <select {...accessibility} value={details.paletteSuggestions === null ? "" : String(details.paletteSuggestions)} onChange={(event) => onChange({ ...details, paletteSuggestions: event.target.value === "" ? null : event.target.value === "true" })}>
            <option value="">Unanswered</option><option value="true">Interested</option><option value="false">Not interested</option>
          </select>
          : <select {...accessibility} value={details[key] ?? ""} onChange={(event) => onChange({ ...details, [key]: event.target.value || null })}>
            <option value="">Unanswered</option>{options[key].map((option) => <option key={option.code} value={option.code}>{option.label}</option>)}
          </select>}
        <p id={`${id}-hint`} className="text-xs leading-relaxed text-muted-foreground">{key === "heightCm" ? "Whole centimeters, or leave blank. No weight or age is collected."
          : key === "garmentSizeNote" ? "Size labels only, up to 60 characters. No measurements or personal history needed."
          : "Choose Unanswered to clear this value."}</p>
        {error && <p id={`${id}-error`} className="text-sm text-destructive">{error}</p>}
      </div>;
    })}</div>
  </fieldset>)}</div>;
}
