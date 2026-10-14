"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState, type ReactNode } from "react";
import { Button } from "@/components/ui/button";
import { useSession } from "./session-provider";

/** Navigation UX only. Spring authorizes every protected API request. */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { session, restore } = useSession();
  const pathname = usePathname();
  const router = useRouter();
  const [retrying, setRetrying] = useState(false);

  useEffect(() => { void restore(); }, [pathname, restore]);
  useEffect(() => {
    if (session.status === "unauthenticated") router.replace("/login");
  }, [session.status, router]);

  if (session.status === "authenticated") return children;
  return (
    <main className="flex min-h-screen flex-1 items-center justify-center bg-background px-6 py-12">
      <div className="w-full max-w-sm space-y-5 text-center">
        {session.status === "error" ? (
          <>
            <p role="alert" className="text-sm text-foreground">{session.message}</p>
            <Button type="button" disabled={retrying} className="h-12 rounded-lg px-6" onClick={async () => {
              setRetrying(true);
              try { await restore(); } finally { setRetrying(false); }
            }}>{retrying ? "Checking…" : "Try again"}</Button>
            <Link href="/login" className="block rounded-sm text-sm text-primary underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-4">Back to sign in</Link>
          </>
        ) : (
          <p role="status" className="text-sm text-muted-foreground">
            {session.status === "loading" ? "Checking your session…" : "Redirecting to sign in…"}
          </p>
        )}
      </div>
    </main>
  );
}
