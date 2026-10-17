import { STEPS, type StepCode } from "@/lib/profile/types";

export function OnboardingProgress({ current }: { current: StepCode }) {
  const index = STEPS.findIndex((step) => step.code === current);
  return (
    <div>
      <p className="mb-3 text-sm font-medium text-muted-foreground">Step {index + 1} of {STEPS.length}</p>
      <ol aria-label="Onboarding progress" className="grid grid-cols-5 gap-2">
        {STEPS.map((step, position) => (
          <li key={step.code} aria-current={step.code === current ? "step" : undefined} className="space-y-2">
            <div aria-hidden="true" className={`h-1 rounded-full ${position <= index ? "bg-primary" : "bg-border"}`} />
            <span className={`hidden text-xs sm:block ${position === index ? "font-semibold text-foreground" : "text-muted-foreground"}`}>{step.title}</span>
            <span className="sr-only sm:hidden">{step.title}</span>
          </li>
        ))}
      </ol>
    </div>
  );
}
