"use client";

import { useRouter } from "next/navigation";
import { useRef, useState } from "react";
import { useSession } from "@/components/auth/session-provider";
import { Button } from "@/components/ui/button";
import { readableError } from "@/lib/api/auth-client";
import { ProfileEntry } from "@/components/profile/profile-workspace";

export default function DashboardPage() {
  const { session, logout } = useSession();
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const submitting = useRef(false);
  if (session.status !== "authenticated" || !session.account) return null;
  const user = session.account;

  async function signOut() {
    if (submitting.current) return;
    submitting.current = true;
    setPending(true);
    setError(null);
    try {
      if (await logout()) router.replace("/login");
    } catch (failure) {
      setError(readableError(failure));
    } finally {
      submitting.current = false;
      setPending(false);
    }
  }

  return (
    <main className="flex min-h-screen flex-1 items-center justify-center bg-background px-6 py-12 sm:px-12">
      <section className="w-full max-w-lg space-y-8" aria-labelledby="welcome-heading">
        <header>
          <p className="mb-3 text-sm font-medium text-muted-foreground">Personal Styling</p>
          <h1 id="welcome-heading" className="break-words font-heading text-3xl leading-tight font-semibold tracking-tight sm:text-4xl">
            Welcome, {user.fullName}
          </h1>
          <p className="mt-3 text-sm leading-relaxed text-muted-foreground">You&apos;re signed in to your account.</p>
        </header>
        <dl className="space-y-4 border-y border-border py-6 text-sm">
          <div><dt className="font-medium">Full name</dt><dd className="mt-1 break-words text-muted-foreground">{user.fullName}</dd></div>
          <div><dt className="font-medium">Email address</dt><dd className="mt-1 break-all text-muted-foreground">{user.email}</dd></div>
          <div><dt className="font-medium">Account ID</dt><dd className="mt-1 text-muted-foreground">{user.id}</dd></div>
        </dl>
        <ProfileEntry />
        {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
        <Button type="button" disabled={pending} onClick={signOut} className="h-12 rounded-lg px-6">
          {pending ? "Signing out…" : "Sign out"}
        </Button>
      </section>
    </main>
  );
}
