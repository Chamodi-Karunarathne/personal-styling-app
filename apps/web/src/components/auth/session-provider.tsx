"use client";

import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from "react";
import { ApiError, authClient, type Account, type LoginRequest, type RegisterRequest } from "@/lib/api/auth-client";

type SessionState = {
  status: "loading" | "authenticated" | "unauthenticated" | "error";
  account: Account | null;
  message: string | null;
};
type SessionContextValue = {
  session: SessionState;
  notice: string | null;
  clearNotice: () => void;
  restore: () => Promise<void>;
  register: (input: RegisterRequest, signal?: AbortSignal) => Promise<boolean>;
  login: (input: LoginRequest, signal?: AbortSignal) => Promise<boolean>;
  logout: () => Promise<boolean>;
};
const SessionContext = createContext<SessionContextValue | null>(null);
const anonymous: SessionState = { status: "unauthenticated", account: null, message: null };

export function SessionProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<SessionState>({ status: "loading", account: null, message: null });
  const [notice, setNotice] = useState<string | null>(null);
  const mounted = useRef(true);
  const version = useRef(0);
  const mutation = useRef(false);
  const hadAccount = useRef(false);
  const restoration = useRef<{ promise: Promise<void>; controller: AbortController } | null>(null);

  const restore = useCallback((): Promise<void> => {
    if (mutation.current) return Promise.resolve();
    if (restoration.current) return restoration.current.promise;
    const currentVersion = version.current;
    const controller = new AbortController();
    const promise = (async () => {
      try {
        const user = await authClient.me(controller.signal);
        if (mounted.current && version.current === currentVersion) {
          hadAccount.current = true;
          setSession({ status: "authenticated", account: user, message: null });
        }
      } catch (error) {
        if (controller.signal.aborted || !mounted.current || version.current !== currentVersion) return;
        if (error instanceof ApiError && error.status === 401) {
          if (hadAccount.current) setNotice("Your session has expired. Please sign in again.");
          hadAccount.current = false;
          setSession(anonymous);
        }
        else setSession({ status: "error", account: null, message: "Unable to verify your session. Please try again." });
      } finally {
        if (restoration.current?.controller === controller) restoration.current = null;
      }
    })();
    restoration.current = { promise, controller };
    return promise;
  }, []);

  useEffect(() => {
    mounted.current = true;
    void restore();
    const onFocus = () => { void restore(); };
    const onVisible = () => { if (document.visibilityState === "visible") void restore(); };
    window.addEventListener("focus", onFocus);
    document.addEventListener("visibilitychange", onVisible);
    return () => {
      mounted.current = false;
      restoration.current?.controller.abort();
      restoration.current = null;
      window.removeEventListener("focus", onFocus);
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, [restore]);

  function beginMutation() {
    if (mutation.current) throw new ApiError(0, "Please wait for the current request to finish.");
    mutation.current = true;
    // Do not abort the HTTP read: serialize it before writes to avoid CSRF cookie races.
    // Its response is ignored once the authentication generation changes.
    version.current++;
  }

  async function register(input: RegisterRequest, signal?: AbortSignal): Promise<boolean> {
    beginMutation();
    try {
      await authClient.register(input, signal);
      if (!mounted.current || signal?.aborted) return false;
      setNotice("Account created. Please sign in.");
      return true;
    } finally {
      mutation.current = false;
      void restore();
    }
  }

  async function login(input: LoginRequest, signal?: AbortSignal): Promise<boolean> {
    beginMutation();
    let succeeded = false;
    try {
      const user = await authClient.login(input, signal);
      if (!mounted.current || signal?.aborted) return false;
      hadAccount.current = true;
      setSession({ status: "authenticated", account: user, message: null });
      setNotice(null);
      succeeded = true;
      return true;
    } finally {
      mutation.current = false;
      if (!succeeded) void restore(); // Resolve uncertain/cancelled login outcomes from the server.
    }
  }

  async function logout(): Promise<boolean> {
    beginMutation();
    let succeeded = false;
    try {
      await authClient.logout();
      if (!mounted.current) return false;
      hadAccount.current = false;
      setSession(anonymous);
      setNotice(null);
      succeeded = true;
      return true;
    } finally {
      mutation.current = false;
      if (!succeeded) void restore(); // A lost logout response may still have invalidated the session.
    }
  }

  return (
    <SessionContext.Provider value={{ session, notice, clearNotice: () => setNotice(null), restore, register, login, logout }}>
      {children}
    </SessionContext.Provider>
  );
}

export function useSession() {
  const context = useContext(SessionContext);
  if (!context) throw new Error("useSession must be used inside SessionProvider.");
  return context;
}
