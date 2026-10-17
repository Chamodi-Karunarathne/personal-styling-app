"use client";

import { useCallback, useEffect, useRef, useState } from "react";

/** A failed attempt must refocus the summary even when its message is unchanged. */
export function useErrorSummary(initial: string | null = null) {
  const [error, updateError] = useState(initial);
  const [attempt, setAttempt] = useState(0);
  const summary = useRef<HTMLDivElement>(null);
  const setError = useCallback((message: string | null) => {
    updateError(message);
    if (message !== null) setAttempt((value) => value + 1);
  }, []);
  useEffect(() => { if (error) summary.current?.focus(); }, [error, attempt]);
  return { error, setError, summary };
}
