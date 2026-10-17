"use client";

import { useEffect, useRef, useState } from "react";
import { useSession } from "@/components/auth/session-provider";
import { Button } from "@/components/ui/button";
import { ApiError, readableError, sessionApiClient, StaleSessionError } from "@/lib/api/session-client";
import { normalizeAppearance, sameAppearance, validateAppearance, type AppearanceSnapshot } from "@/lib/appearance/types";
import { AppearanceFields } from "./appearance-fields";
import { useAppearanceResource } from "./use-appearance-resource";

export function AppearanceEditor({ onStateChange }: { onStateChange: (state: { dirty: boolean; pending: boolean }) => void }) {
  const { session } = useSession();
  if (!session.account || session.status !== "authenticated") return null;
  return <OwnedAppearanceEditor key={`${session.account.id}:${sessionApiClient.epoch()}`} accountId={session.account.id} onStateChange={onStateChange} />;
}
function OwnedAppearanceEditor({ accountId, onStateChange }: { accountId: number; onStateChange: (state: { dirty: boolean; pending: boolean }) => void }) {
  const resource = useAppearanceResource(accountId);
  const [revision, setRevision] = useState(0);
  const [retryError, setRetryError] = useState<string | null>(null);
  async function reload() {
    try { await resource.reload(); setRetryError(null); setRevision((value) => value + 1); }
    catch (failure) { setRetryError(readableError(failure)); }
  }
  return <section aria-labelledby="personal-details-heading" className="mt-12 space-y-6 border-t border-border pt-10">
    <h2 id="personal-details-heading" className="font-heading text-2xl font-semibold sm:text-3xl">Personal Details &amp; Fit</h2>
    <p className="text-sm leading-relaxed text-muted-foreground">Entirely optional. These details may help future sizing, color, and accessory suggestions. They are not used to rate appearance or restrict outfits, and are not sent to AI or external services.</p>
    {!resource.snapshot ? resource.loadError || retryError ? <div role="alert" className="space-y-3"><p className="text-sm text-destructive">{retryError ?? resource.loadError}</p><Button type="button" variant="outline" onClick={() => void reload()}>Retry personal details</Button></div>
      : <p role="status" className="text-sm text-muted-foreground">Loading personal details…</p>
      : <AppearanceEditorForm key={revision} initial={resource.snapshot} resource={resource} onReload={reload} onStateChange={onStateChange} />}
  </section>;
}
function AppearanceEditorForm({ initial, resource, onReload, onStateChange }: {
  initial: AppearanceSnapshot; resource: ReturnType<typeof useAppearanceResource>; onReload: () => Promise<void>;
  onStateChange: (state: { dirty: boolean; pending: boolean }) => void;
}) {
  const [snapshot, setSnapshot] = useState(initial);
  const [details, setDetails] = useState(() => structuredClone(initial.details));
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [removing, setRemoving] = useState(false);
  const [blocked, setBlocked] = useState(false);
  const submitting = useRef(false); const mounted = useRef(true); const summary = useRef<HTMLDivElement>(null);
  const dirty = !sameAppearance(details, snapshot.details);
  useEffect(() => { mounted.current = true; return () => { mounted.current = false; }; }, []);
  useEffect(() => { onStateChange({ dirty, pending }); }, [dirty, pending, onStateChange]);
  useEffect(() => { if (error) summary.current?.focus(); }, [error]);
  useEffect(() => {
    if (!dirty) return; const warn = (event: BeforeUnloadEvent) => event.preventDefault(); window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [dirty]);
  async function run(deleting: boolean) {
    if (submitting.current || blocked) return;
    if (deleting && !window.confirm("Delete all personal details? Your styling preferences and account will be kept.")) return;
    const validation = deleting ? {} : validateAppearance(details, snapshot.options);
    setErrors(validation); setMessage(null); setError(null);
    if (Object.keys(validation).length) { setError("Please check your personal details."); return; }
    submitting.current = true; setRemoving(deleting); setPending(true);
    try {
      const saved = deleting ? await resource.remove(snapshot.version) : await resource.save({ version: snapshot.version, details: normalizeAppearance(details) });
      if (!mounted.current) return;
      setSnapshot(saved); setDetails(structuredClone(saved.details));
      setMessage(deleting ? "Personal details deleted. Your styling preferences are unchanged." : "Your personal details have been saved.");
    } catch (failure) {
      if (!mounted.current) return;
      if (failure instanceof ApiError) { setErrors(failure.fieldErrors); setBlocked(failure.status === 409 || failure.uncertain || failure instanceof StaleSessionError); }
      setError(readableError(failure));
    } finally { submitting.current = false; if (mounted.current) { setPending(false); setRemoving(false); } }
  }
  return <form noValidate aria-busy={pending} onSubmit={(event) => { event.preventDefault(); void run(false); }} className="space-y-6">
    <AppearanceFields details={details} options={snapshot.options} onChange={(value) => { setDetails(value); setErrors({}); setMessage(null); }} errors={errors} disabled={pending} />
    {error && <div ref={summary} role="alert" tabIndex={-1} className="space-y-3 rounded-lg border border-destructive/30 p-4 text-sm text-destructive outline-none"><p>{error}</p>{blocked && <Button type="button" variant="outline" onClick={() => { if (window.confirm("Reloading replaces unsaved personal details. Continue?")) void onReload(); }}>Reload personal details</Button>}</div>}
    {message && <p role="status" className="text-sm text-muted-foreground">{message}</p>}
    <div className="flex flex-wrap gap-3">
      <Button type="button" variant="outline" disabled={pending} onClick={() => { if (!dirty || window.confirm("Discard your unsaved personal details changes?")) { setDetails(structuredClone(snapshot.details)); setErrors({}); setMessage("Unsaved personal details changes discarded."); if (!blocked) setError(null); } }} className="h-12 rounded-lg">Cancel personal details changes</Button>
      <Button type="submit" disabled={pending || blocked} className="h-12 rounded-lg">{pending && !removing ? "Saving…" : "Save personal details"}</Button>
    </div>
    <div className="border-t border-border pt-5"><Button type="button" variant="outline" disabled={pending || blocked || snapshot.version === null} onClick={() => void run(true)} className="min-h-12 whitespace-normal rounded-lg text-destructive">{pending && removing ? "Deleting…" : "Delete personal details"}</Button></div>
  </form>;
}
