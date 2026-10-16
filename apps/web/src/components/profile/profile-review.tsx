import { Button } from "@/components/ui/button";
import { STEPS, type PreferenceCatalog, type Preferences, type StepCode } from "@/lib/profile/types";

export function ProfileReview({ preferences, catalog, onEdit, disabled = false }: {
  preferences: Preferences;
  catalog: PreferenceCatalog;
  onEdit?: (step: StepCode) => void;
  disabled?: boolean;
}) {
  return (
    <dl className="divide-y divide-border border-y border-border">
      {catalog.groups.map((group) => {
        const values = preferences[group.key];
        const step = STEPS.find((candidate) => candidate.groups.includes(group.key));
        const labels = (values ?? []).map((choice) => {
          const code = typeof choice === "string" ? choice : choice.code;
          const label = group.options.find((option) => option.code === code)?.label ?? code;
          return typeof choice === "string" ? label : `${label} (${choice.strength === "REQUIRED" ? "Required" : "Preferred"})`;
        });
        return (
          <div key={group.key} className="flex flex-wrap items-start justify-between gap-2 py-4">
            <div className="min-w-0 flex-1">
              <dt className="text-sm font-medium">{group.label}</dt>
              <dd className="mt-1 break-words text-sm leading-relaxed text-muted-foreground">
                {values === null ? "Unanswered" : values.length === 0 ? "No preference" : labels.join(", ")}
              </dd>
            </div>
            {onEdit && step && <Button type="button" variant="link" disabled={disabled} onClick={() => onEdit(step.code)} className="min-h-11 px-2" aria-label={`Edit ${group.label.toLowerCase()}`}>Edit</Button>}
          </div>
        );
      })}
    </dl>
  );
}
