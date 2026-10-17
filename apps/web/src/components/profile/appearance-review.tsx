import { APPEARANCE_KEYS, type AppearanceDetails, type AppearanceOptions } from "@/lib/appearance/types";
import { Button } from "@/components/ui/button";
import { APPEARANCE_LABELS } from "./appearance-fields";

export function AppearanceReview({ details, options, onEdit, disabled }: { details: AppearanceDetails | null; options?: AppearanceOptions; onEdit: () => void; disabled: boolean }) {
  return <section className="mt-8 space-y-4 border-t border-border pt-6" aria-labelledby="appearance-review-heading">
    <div className="flex flex-wrap items-center justify-between gap-3"><h2 id="appearance-review-heading" className="font-heading text-2xl font-semibold">Personal Details &amp; Fit</h2><Button type="button" variant="link" disabled={disabled} onClick={onEdit}>Edit personal details</Button></div>
    {details ? <dl className="grid min-w-0 gap-4 text-sm sm:grid-cols-2">{APPEARANCE_KEYS.map((key) => {
      const value = details[key];
      const label = value === null ? "Unanswered" : key === "heightCm" ? `${value} cm`
        : key === "paletteSuggestions" ? value ? "Interested" : "Not interested"
        : key === "garmentSizeNote" ? String(value) : options?.[key]?.find((option) => option.code === value)?.label ?? "Unanswered";
      return <div key={key} className="min-w-0"><dt className="font-medium">{APPEARANCE_LABELS[key]}</dt><dd className="mt-1 break-words text-muted-foreground">{label}</dd></div>;
    })}</dl> : <p className="text-sm text-muted-foreground">Personal details are not loaded. Saved personal details have not been changed.</p>}
  </section>;
}
