"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { PreferenceFields } from "@/components/profile/preference-fields";
import { ProfileReview } from "@/components/profile/profile-review";
import { ApiError, readableError } from "@/lib/api/auth-client";
import { INITIAL_CATALOG } from "@/lib/profile/catalog";
import { STEPS, clonePreferences, emptyPreferences, type PreferenceCatalog, type ProfileSnapshot, type ProfileWrite, type StepCode } from "@/lib/profile/types";
import { validatePreferences } from "@/lib/profile/validation";
import { OnboardingProgress } from "./onboarding-progress";

type OnboardingFlowProps = {
  catalog?: PreferenceCatalog;
  initial?: ProfileSnapshot;
  onSave?: (input: ProfileWrite) => Promise<ProfileSnapshot>;
  onExit?: () => void;
  onReload?: () => void;
};

export function OnboardingFlow({ catalog = INITIAL_CATALOG, initial, onSave, onExit, onReload }: OnboardingFlowProps) {
  const [preferences, setPreferences] = useState(() => initial ? clonePreferences(initial.preferences) : emptyPreferences());
  const [stepCode, setStepCode] = useState<StepCode>(initial?.lastVisitedStep ?? "STYLE");
  const [version, setVersion] = useState(initial?.version ?? null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [conflict, setConflict] = useState(false);
  const [pending, setPending] = useState(false);
  const [savedPreferences, setSavedPreferences] = useState(() => JSON.stringify(initial?.preferences ?? emptyPreferences()));
  const submitting = useRef(false);
  const mounted = useRef(true);
  const heading = useRef<HTMLHeadingElement>(null);
  const errorSummary = useRef<HTMLDivElement>(null);
  const firstRender = useRef(true);
  const stepIndex = STEPS.findIndex((step) => step.code === stepCode);
  const step = STEPS[stepIndex];
  const dirty = JSON.stringify(preferences) !== savedPreferences;

  useEffect(() => { mounted.current = true; return () => { mounted.current = false; }; }, []);
  useEffect(() => {
    if (firstRender.current) { firstRender.current = false; return; }
    heading.current?.focus();
  }, [stepCode]);
  useEffect(() => { if (error) errorSummary.current?.focus(); }, [error]);
  useEffect(() => {
    if (!dirty) return;
    const warn = (event: BeforeUnloadEvent) => { event.preventDefault(); };
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [dirty]);

  function moveTo(code: StepCode) {
    setErrors({}); if (!conflict) setError(null); setMessage(null); setStepCode(code);
  }

  async function save(intent: ProfileWrite["intent"], destination: StepCode, exit = false) {
    if (submitting.current || conflict) return;
    const validation = validatePreferences(preferences, catalog);
    setErrors(validation); setError(null); setMessage(null); setConflict(false);
    if (Object.keys(validation).length) {
      setError("Please check your choices before continuing.");
      const invalidStep = STEPS.find((candidate) => candidate.groups.some((key) => validation[key] || validation[`preferences.${key}`]));
      if (invalidStep) setStepCode(invalidStep.code);
      return;
    }
    if (!onSave) {
      if (destination !== stepCode && !exit) setStepCode(destination);
      setMessage(exit
        ? "Preview only: your choices are still in this tab and have not been saved to the server."
        : "Preview only: changes stay in this tab and have not been saved to the server.");
      return;
    }
    submitting.current = true;
    setPending(true);
    try {
      const saved = await onSave({ version, intent, lastVisitedStep: destination, preferences: clonePreferences(preferences) });
      if (!mounted.current) return;
      setVersion(saved.version);
      setPreferences(clonePreferences(saved.preferences));
      setSavedPreferences(JSON.stringify(saved.preferences));
      if (exit) onExit?.();
      else { setStepCode(destination); setMessage("Your progress has been saved."); }
    } catch (failure) {
      if (!mounted.current) return;
      if (failure instanceof ApiError) {
        setErrors(failure.fieldErrors);
        setConflict(failure.status === 409);
      }
      setError(readableError(failure));
    } finally {
      submitting.current = false;
      if (mounted.current) setPending(false);
    }
  }

  return (
    <main className="min-h-screen flex-1 bg-background px-5 py-8 sm:px-8 sm:py-12">
      <section className="mx-auto w-full max-w-3xl" aria-labelledby="onboarding-heading" aria-busy={pending}>
        <header className="mb-8 flex flex-wrap items-center justify-between gap-4">
          <span className="text-sm font-medium text-muted-foreground">Personal Styling</span>
          <Link href="/dashboard" onClick={(event) => { if (pending || (dirty && !window.confirm("Leave without saving your latest choices?"))) event.preventDefault(); }} className="rounded-sm py-2 text-sm text-primary underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4">Back to dashboard</Link>
        </header>
        <OnboardingProgress current={stepCode} />
        <div className="my-8 sm:my-10">
          <h1 ref={heading} tabIndex={-1} id="onboarding-heading" className="font-heading text-3xl leading-tight font-semibold tracking-tight outline-none sm:text-4xl">{step.title}</h1>
          <p className="mt-3 max-w-2xl text-base leading-relaxed text-muted-foreground">{step.description}</p>
          <p className="mt-3 text-sm leading-relaxed text-muted-foreground">All answers are optional. Unanswered choices and No preference are kept separate. You can change your preferences later.</p>
          {!onSave && <p className="mt-4 rounded-lg border border-border bg-muted/40 px-4 py-3 text-sm leading-relaxed">This is a preview. Choices stay in this tab only; saving to your account is not connected yet.</p>}
        </div>
        <form noValidate onSubmit={(event) => { event.preventDefault(); void save(stepCode === "REVIEW" ? "COMPLETE" : "SAVE", STEPS[Math.min(stepIndex + 1, STEPS.length - 1)].code, stepCode === "REVIEW"); }}>
          {stepCode === "REVIEW"
            ? <ProfileReview preferences={preferences} catalog={catalog} onEdit={moveTo} disabled={pending} />
            : <PreferenceFields preferences={preferences} catalog={catalog} keys={step.groups} onChange={(next) => { setPreferences(next); setErrors({}); setMessage(null); }} errors={errors} disabled={pending} />}
          {error && <div ref={errorSummary} role="alert" tabIndex={-1} className="mt-6 space-y-2 rounded-lg border border-destructive/30 px-4 py-3 text-sm text-destructive outline-none"><p>{error}</p>{conflict && <p>Your changes are still here. Reload the saved profile before making another save.</p>}{conflict && onReload && <Button type="button" variant="outline" onClick={() => { if (window.confirm("Reloading replaces your unsaved choices. Continue?")) onReload(); }}>Reload saved profile</Button>}</div>}
          {message && <p role="status" className="mt-6 text-sm leading-relaxed text-muted-foreground">{message}</p>}
          <div className="mt-8 flex flex-wrap items-center justify-between gap-3 border-t border-border pt-6">
            <Button type="button" variant="outline" disabled={pending || stepIndex === 0} onClick={() => moveTo(STEPS[stepIndex - 1].code)} className="h-12 rounded-lg px-6">Back</Button>
            <Button type="submit" disabled={pending || conflict} className="h-12 rounded-lg px-6">{pending ? "Saving…" : stepCode === "REVIEW" ? "Finish onboarding" : "Next"}</Button>
          </div>
          <div className="mt-4 flex flex-wrap items-center justify-between gap-x-5 gap-y-1">
            <Button type="button" variant="link" disabled={pending || conflict} onClick={() => void save("SAVE", stepCode, true)} className="min-h-11 px-0">Save and exit</Button>
            <Button type="button" variant="link" disabled={pending || conflict} onClick={() => void save("SKIP", stepCode, true)} className="min-h-11 px-0">Skip onboarding</Button>
          </div>
        </form>
      </section>
    </main>
  );
}
