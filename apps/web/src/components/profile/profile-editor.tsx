"use client";

import Link from "next/link";
import { useEffect, useRef, useState, type ReactNode } from "react";
import { useSession } from "@/components/auth/session-provider";
import { Button } from "@/components/ui/button";
import { ApiError, readableError } from "@/lib/api/auth-client";
import { StaleSessionError } from "@/lib/api/session-client";
import { INITIAL_CATALOG } from "@/lib/profile/catalog";
import { GROUP_KEYS, clonePreferences, emptyProfile, type PreferenceCatalog, type ProfileSnapshot, type ProfileWrite } from "@/lib/profile/types";
import { validatePreferences } from "@/lib/profile/validation";
import { PreferenceFields } from "./preference-fields";

type ProfileEditorProps = {
  catalog?: PreferenceCatalog;
  initial?: ProfileSnapshot;
  onSave?: (input: ProfileWrite) => Promise<ProfileSnapshot>;
  onExit?: () => void;
  onReload?: () => void;
  loading?: boolean;
  loadError?: string | null;
  onRetry?: () => void;
  personalDetails?: ReactNode;
  otherDirty?: boolean;
  otherPending?: boolean;
};

export function ProfileEditor({ catalog = INITIAL_CATALOG, initial, onSave, onExit, onReload, loading = false, loadError = null, onRetry, personalDetails, otherDirty = false, otherPending = false }: ProfileEditorProps) {
  const { session } = useSession();
  const [snapshot, setSnapshot] = useState<ProfileSnapshot>(() => initial ?? emptyProfile());
  const [preferences, setPreferences] = useState(() => clonePreferences(initial?.preferences ?? emptyProfile().preferences));
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [conflict, setConflict] = useState(false);
  const [pending, setPending] = useState(false);
  const submitting = useRef(false);
  const mounted = useRef(true);
  const errorSummary = useRef<HTMLDivElement>(null);
  const dirty = JSON.stringify(preferences) !== JSON.stringify(snapshot.preferences);

  useEffect(() => { mounted.current = true; return () => { mounted.current = false; }; }, []);
  useEffect(() => { if (error) errorSummary.current?.focus(); }, [error]);
  useEffect(() => {
    if (!dirty) return;
    const warn = (event: BeforeUnloadEvent) => { event.preventDefault(); };
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [dirty]);

  async function save() {
    if (submitting.current || conflict) return;
    const validation = validatePreferences(preferences, catalog);
    setErrors(validation); setError(null); setMessage(null);
    if (Object.keys(validation).length) {
      setError("Please check your choices before saving.");
      return;
    }
    if (!onSave) {
      setMessage("Preview only: your choices have not been saved to the server. Cancel can still discard these changes.");
      return;
    }
    submitting.current = true;
    setPending(true);
    try {
      const saved = await onSave({ version: snapshot.version, intent: "SAVE", lastVisitedStep: snapshot.lastVisitedStep,
        appearanceStepPending: snapshot.appearanceStepPending === true, preferences: clonePreferences(preferences) });
      if (!mounted.current) return;
      setSnapshot(saved);
      setPreferences(clonePreferences(saved.preferences));
      setMessage("Your styling preferences have been saved.");
    } catch (failure) {
      if (!mounted.current) return;
      if (failure instanceof ApiError) { setErrors(failure.fieldErrors); setConflict(failure.status === 409 || failure.uncertain || failure instanceof StaleSessionError); }
      setError(readableError(failure));
    } finally {
      submitting.current = false;
      if (mounted.current) setPending(false);
    }
  }

  function cancel() {
    if (pending || (dirty && !window.confirm("Discard your unsaved preference changes?"))) return;
    setPreferences(clonePreferences(snapshot.preferences));
    setErrors({});
    if (!conflict) setError(null);
    setMessage("Your unsaved changes have been discarded.");
    onExit?.();
  }

  return (
    <main className="min-h-screen flex-1 bg-background px-5 py-8 sm:px-8 sm:py-12">
      <section className="mx-auto w-full max-w-3xl" aria-labelledby="profile-heading" aria-busy={pending || loading}>
        <header className="mb-8 flex flex-wrap items-center justify-between gap-4">
          <span className="text-sm font-medium text-muted-foreground">Personal Styling</span>
          <Link href="/dashboard" onClick={(event) => { if (pending || otherPending || ((dirty || otherDirty) && !window.confirm("Leave without saving your latest choices?"))) event.preventDefault(); }} className="rounded-sm py-2 text-sm text-primary underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4">Back to dashboard</Link>
        </header>
        <h1 id="profile-heading" className="font-heading text-3xl leading-tight font-semibold tracking-tight sm:text-4xl">Your styling profile</h1>
        <p className="mt-3 text-base leading-relaxed text-muted-foreground">Make these preferences your own. Every answer is optional, and you can change it later.</p>
        {session.account && <div className="my-8 space-y-4 border-y border-border py-5">
          <dl className="grid min-w-0 gap-4 text-sm sm:grid-cols-2">
            <div className="min-w-0"><dt className="font-medium">Account name</dt><dd className="mt-1 break-words text-muted-foreground">{session.account.fullName}</dd></div>
            <div className="min-w-0"><dt className="font-medium">Email address</dt><dd className="mt-1 break-all text-muted-foreground">{session.account.email}</dd></div>
          </dl>
          <p className="text-xs text-muted-foreground">Account identity and password changes are outside styling preferences.</p>
        </div>}
        {loading ? <p role="status" className="my-8 text-sm text-muted-foreground">Loading your styling profile…</p>
          : loadError ? <div role="alert" className="my-8 space-y-4"><p className="text-sm text-destructive">{loadError}</p>{onRetry && <Button type="button" variant="outline" onClick={onRetry}>Try again</Button>}</div>
            : <>
              {snapshot.onboardingStatus === "NOT_STARTED" && <p className="mb-6 text-sm leading-relaxed text-muted-foreground">You haven&apos;t saved a styling profile yet. Leave groups unanswered or choose No preference explicitly.</p>}
              {!onSave && <p className="mb-8 rounded-lg border border-border bg-muted/40 px-4 py-3 text-sm leading-relaxed">This is a preview. Choices stay in this tab only; saving to your account is not connected yet.</p>}
              <form noValidate onSubmit={(event) => { event.preventDefault(); void save(); }}>
                <PreferenceFields preferences={preferences} catalog={catalog} keys={GROUP_KEYS} onChange={(next) => { setPreferences(next); setErrors({}); setMessage(null); }} errors={errors} disabled={pending} />
                {error && <div ref={errorSummary} tabIndex={-1} role="alert" className="mt-6 space-y-2 rounded-lg border border-destructive/30 px-4 py-3 text-sm text-destructive outline-none"><p>{error}</p>{conflict && <p>Your edits are still here. Reload the saved profile before saving again.</p>}{conflict && onReload && <Button type="button" variant="outline" onClick={() => { if (window.confirm("Reloading replaces your unsaved choices. Continue?")) onReload(); }}>Reload saved profile</Button>}</div>}
                {message && <p role="status" className="mt-6 text-sm leading-relaxed text-muted-foreground">{message}</p>}
                <div className="mt-8 flex flex-wrap items-center justify-between gap-3 border-t border-border pt-6">
                  <Button type="button" variant="outline" disabled={pending} onClick={cancel} className="h-12 rounded-lg px-6">Cancel</Button>
                  <Button type="submit" disabled={pending || conflict} className="h-12 rounded-lg px-6">{pending ? "Saving…" : "Save preferences"}</Button>
                </div>
              </form>
            </>}
        {personalDetails}
      </section>
    </main>
  );
}
