"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { useSession } from "@/components/auth/session-provider";
import { OnboardingFlow } from "@/components/onboarding/onboarding-flow";
import { Button } from "@/components/ui/button";
import { profileClient } from "@/lib/api/profile-client";
import { ApiError, readableError, sessionApiClient, StaleSessionError } from "@/lib/api/session-client";
import { confirmsWrite } from "@/lib/profile/reconciliation";
import type { PreferenceCatalog, ProfileSnapshot, ProfileWrite } from "@/lib/profile/types";
import { ProfileEditor } from "./profile-editor";

function useOwnedProfile(accountId: number, includeCatalog: boolean) {
  const { restore } = useSession();
  const [client] = useState(() => profileClient.forAccount(accountId));
  const [data, setData] = useState<{ profile: ProfileSnapshot; catalog: PreferenceCatalog | null } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const operations = useRef<AbortController | null>(null);
  useEffect(() => {
    let current = true;
    const readController = new AbortController();
    void (async () => {
      try {
        const [profile, catalog] = await Promise.all([client.me(readController.signal), includeCatalog ? client.options(readController.signal) : null]);
        if (current && client.isCurrent()) setData({ profile, catalog });
      } catch (failure) {
        if (!current || readController.signal.aborted) return;
        if (failure instanceof StaleSessionError || (failure instanceof ApiError && failure.status === 401)) void restore();
        setError(readableError(failure));
      }
    })();
    return () => { current = false; readController.abort(); };
  }, [client, includeCatalog, restore]);
  useEffect(() => {
    const controller = new AbortController();
    operations.current = controller;
    return () => { controller.abort(); };
  }, []);
  return { client, data, error, operations, restore };
}

export function ProfileWorkspace({ mode }: { mode: "onboarding" | "editor" }) {
  const { session } = useSession();
  const [revision, setRevision] = useState(0);
  if (session.status !== "authenticated" || !session.account) return null;
  return <OwnedWorkspace key={`${session.account.id}:${sessionApiClient.epoch()}:${revision}`} accountId={session.account.id}
    mode={mode} reload={() => setRevision((value) => value + 1)} />;
}

function OwnedWorkspace({ accountId, mode, reload }: { accountId: number; mode: "onboarding" | "editor"; reload: () => void }) {
  const { client, data, error, operations, restore } = useOwnedProfile(accountId, true);
  const router = useRouter();
  async function save(input: ProfileWrite): Promise<ProfileSnapshot> {
    const controller = operations.current!;
    try { return await client.save(input, controller.signal); }
    catch (failure) {
      if (failure instanceof StaleSessionError || (failure instanceof ApiError && failure.status === 401)) {
        void restore(); throw failure;
      }
      if (failure instanceof ApiError && failure.uncertain && !controller.signal.aborted) {
        try {
          const saved = await client.me(controller.signal);
          if (confirmsWrite(saved, input, data!.profile.onboardingStatus)) return saved;
        } catch (readFailure) {
          if (readFailure instanceof StaleSessionError || (readFailure instanceof ApiError && readFailure.status === 401)) {
            void restore(); throw readFailure;
          }
        }
        throw new ApiError(0, "We couldn't confirm whether your changes were saved. Your edits are still here. Reload the saved profile before continuing.", {}, true);
      }
      throw failure;
    }
  }
  if (!data || !data.catalog) return <main className="min-h-screen flex-1 bg-background px-5 py-12 sm:px-8">
    <section className="mx-auto max-w-3xl space-y-6">
      <h1 className="font-heading text-3xl font-semibold">{mode === "onboarding" ? "Your styling preferences" : "Your styling profile"}</h1>
      {error ? <div role="alert" className="space-y-4"><p className="text-sm text-destructive">{error}</p><Button type="button" variant="outline" onClick={reload}>Try again</Button></div>
        : <p role="status" className="text-sm text-muted-foreground">Loading your styling profile…</p>}
      <Link href="/dashboard" className="inline-block rounded-sm py-3 text-sm text-primary underline focus-visible:outline-2 focus-visible:outline-offset-4">Back to dashboard</Link>
    </section>
  </main>;
  const props = { initial: data.profile, catalog: data.catalog, onSave: save, onReload: reload };
  return mode === "onboarding" ? <OnboardingFlow {...props} onExit={() => router.push("/dashboard")} /> : <ProfileEditor {...props} />;
}

export function ProfileEntry() {
  const { session } = useSession();
  const [revision, setRevision] = useState(0);
  if (session.status !== "authenticated" || !session.account) return null;
  return <OwnedEntry key={`${session.account.id}:${sessionApiClient.epoch()}:${revision}`} accountId={session.account.id} retry={() => setRevision((value) => value + 1)} />;
}
function OwnedEntry({ accountId, retry }: { accountId: number; retry: () => void }) {
  const { data, error } = useOwnedProfile(accountId, false);
  const status = data?.profile.onboardingStatus;
  return <section className="space-y-3 border-b border-border pb-6" aria-labelledby="styling-setup-heading">
    <h2 id="styling-setup-heading" className="font-heading text-2xl font-semibold">Your styling preferences</h2>
    {error ? <><p role="alert" className="text-sm text-destructive">{error}</p><Button type="button" variant="outline" onClick={retry}>Retry styling profile</Button></>
      : !data ? <p role="status" className="text-sm text-muted-foreground">Loading styling preferences…</p>
      : <>
        <p className="text-sm leading-relaxed text-muted-foreground">{status === "COMPLETED" ? "Your preferences are saved. You can change them whenever you like."
          : status === "SKIPPED" ? "You skipped setup. Your saved choices are kept, and you can return whenever you like."
          : status === "IN_PROGRESS" ? "Pick up where you left off. All preference answers are optional."
          : "Set your style, comfort, and lifestyle preferences. You can skip any answer."}</p>
        <Link href={status === "COMPLETED" ? "/profile" : "/onboarding"} className="inline-block rounded-sm py-3 text-sm font-medium text-primary underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-4">{status === "COMPLETED" ? "Edit preferences" : status === "NOT_STARTED" ? "Start setup" : "Resume setup"}</Link>
        {status !== "COMPLETED" && <Link href="/profile" className="ml-5 inline-block rounded-sm py-3 text-sm text-primary underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-4">Edit preferences</Link>}
      </>}
  </section>;
}
