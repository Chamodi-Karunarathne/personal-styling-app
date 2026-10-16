"use client";

import { Button } from "@/components/ui/button";
import type { PreferenceCatalog, PreferenceKey, Preferences } from "@/lib/profile/types";

type PreferenceFieldsProps = {
  preferences: Preferences;
  catalog: PreferenceCatalog;
  keys: readonly PreferenceKey[];
  onChange: (preferences: Preferences) => void;
  errors?: Record<string, string>;
  disabled?: boolean;
};

/** Native controls keep the same preference semantics in setup and the editor. */
export function PreferenceFields({ preferences, catalog, keys, onChange, errors = {}, disabled = false }: PreferenceFieldsProps) {
  function toggle(key: PreferenceKey, code: string, selected: boolean) {
    if (key === "coverage") {
      const choices = preferences.coverage ?? [];
      onChange({ ...preferences, coverage: selected
        ? [...choices, { code, strength: "PREFERENCE" }]
        : choices.filter((choice) => choice.code !== code) });
      return;
    }
    const choices = preferences[key] ?? [];
    onChange({ ...preferences, [key]: selected ? [...choices, code] : choices.filter((choice) => choice !== code) });
  }

  return (
    <div className="space-y-9">
      {keys.map((key) => {
        const group = catalog.groups.find((candidate) => candidate.key === key);
        if (!group) return null;
        const values = preferences[key];
        const noPreference = values !== null && values.length === 0;
        const selectedCodes = (values ?? []).map((choice) => typeof choice === "string" ? choice : choice.code);
        const error = errors[key] ?? errors[`preferences.${key}`];
        const descriptionId = `preference-${key}-description`;
        const errorId = `preference-${key}-error`;

        return (
          <fieldset key={key} disabled={disabled} aria-describedby={[descriptionId, error ? errorId : null].filter(Boolean).join(" ")} className="min-w-0 space-y-3">
            <legend className="text-base font-semibold">{group.label}{!group.label.toLowerCase().includes("optional") && <> <span className="text-sm font-normal text-muted-foreground">(optional)</span></>}</legend>
            <p id={descriptionId} className="text-sm leading-relaxed text-muted-foreground">
              {key === "coverage" ? "Choose only what matters to you. Required choices will be kept distinct for future recommendations." : "Select any that suit you, or choose No preference."}
            </p>
            <div className="grid grid-cols-1 gap-2 min-[420px]:grid-cols-2 sm:grid-cols-3">
              {group.options.map((option) => {
                const checked = selectedCodes.includes(option.code);
                const id = `preference-${key}-${option.code}`;
                return (
                  <label key={option.code} htmlFor={id} className={`flex min-h-12 min-w-0 cursor-pointer items-center gap-3 rounded-lg border px-3 py-3 text-sm transition-colors motion-reduce:transition-none ${checked ? "border-primary bg-primary/5" : "border-input bg-background hover:bg-muted/40"}`}>
                    <input id={id} type="checkbox" checked={checked} onChange={(event) => toggle(key, option.code, event.target.checked)} aria-invalid={error ? true : undefined} className="size-4 shrink-0 accent-primary focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary" />
                    {option.swatch && <span aria-hidden="true" className="size-4 shrink-0 rounded-full border border-foreground/20" style={{ backgroundColor: option.swatch }} />}
                    <span className="min-w-0 break-words">{option.label}</span>
                  </label>
                );
              })}
            </div>
            {key === "coverage" && (preferences.coverage ?? []).map((choice) => {
              const label = group.options.find((option) => option.code === choice.code)?.label ?? choice.code;
              const id = `coverage-strength-${choice.code}`;
              return (
                <div key={choice.code} className="flex flex-wrap items-center gap-x-4 gap-y-2 rounded-lg bg-muted/40 p-3">
                  <label htmlFor={id} className="min-w-0 flex-1 text-sm">How important is {label.toLowerCase()}?</label>
                  <select id={id} value={choice.strength} onChange={(event) => onChange({ ...preferences, coverage: (preferences.coverage ?? []).map((entry) => entry.code === choice.code ? { ...entry, strength: event.target.value as "PREFERENCE" | "REQUIRED" } : entry) })} className="h-11 max-w-full rounded-lg border border-input bg-background px-3 text-base focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary">
                    <option value="PREFERENCE">Preferred</option>
                    <option value="REQUIRED">Required</option>
                  </select>
                </div>
              );
            })}
            <div className="flex flex-wrap items-center gap-x-5 gap-y-2 pt-1">
              <label htmlFor={`preference-${key}-none`} className="flex min-h-11 cursor-pointer items-center gap-2 text-sm">
                <input id={`preference-${key}-none`} type="checkbox" checked={noPreference} onChange={(event) => onChange({ ...preferences, [key]: event.target.checked ? [] : null })} className="size-4 accent-primary focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary" />
                No preference
              </label>
              <Button type="button" variant="link" disabled={disabled || values === null} onClick={() => onChange({ ...preferences, [key]: null })} className="min-h-11 whitespace-normal px-0 text-sm">Clear choices</Button>
              {values === null && <span className="text-xs text-muted-foreground">Unanswered</span>}
            </div>
            {error && <p id={errorId} className="text-sm text-destructive">{error}</p>}
          </fieldset>
        );
      })}
    </div>
  );
}
