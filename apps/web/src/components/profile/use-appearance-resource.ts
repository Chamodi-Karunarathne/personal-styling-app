"use client";

import { useEffect, useRef, useState } from "react";
import { useSession } from "@/components/auth/session-provider";
import { profileClient } from "@/lib/api/profile-client";
import { ApiError, readableError, StaleSessionError } from "@/lib/api/session-client";
import { emptyAppearance, sameAppearance, type AppearanceSnapshot, type AppearanceWrite } from "@/lib/appearance/types";

export function useAppearanceResource(accountId: number) {
  const { restore } = useSession();
  const [client] = useState(() => profileClient.forAccount(accountId));
  const [snapshot, setSnapshot] = useState<AppearanceSnapshot | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const operations = useRef<AbortController | null>(null);
  function handleFailure(failure: unknown) {
    if (failure instanceof StaleSessionError || (failure instanceof ApiError && failure.status === 401)) void restore();
  }
  useEffect(() => {
    const controller = new AbortController(); operations.current = controller;
    void client.appearance(controller.signal).then((value) => {
      if (!controller.signal.aborted && client.isCurrent()) setSnapshot(value);
    }).catch((failure: unknown) => {
      if (controller.signal.aborted) return;
      if (failure instanceof StaleSessionError || (failure instanceof ApiError && failure.status === 401)) void restore();
      setLoadError(readableError(failure));
    });
    return () => controller.abort();
  }, [client, restore]);
  async function reload(): Promise<AppearanceSnapshot> {
    try {
      const value = await client.appearance(operations.current!.signal);
      setSnapshot(value); setLoadError(null); return value;
    } catch (failure) { handleFailure(failure); setLoadError(readableError(failure)); throw failure; }
  }
  async function save(input: AppearanceWrite): Promise<AppearanceSnapshot> {
    const signal = operations.current!.signal;
    try {
      const value = await client.saveAppearance(input, signal); setSnapshot(value); return value;
    } catch (failure) {
      handleFailure(failure);
      if (failure instanceof ApiError && failure.uncertain && !signal.aborted) {
        try {
          const value = await client.appearance(signal);
          if (value.version !== null && value.version !== input.version && sameAppearance(value.details, input.details)) { setSnapshot(value); return value; }
        } catch (readFailure) { handleFailure(readFailure); if (readFailure instanceof StaleSessionError || (readFailure instanceof ApiError && readFailure.status === 401)) throw readFailure; }
        throw new ApiError(0, "We couldn't confirm the personal details save. Your edits are kept. Reload before continuing.", {}, true);
      }
      throw failure;
    }
  }
  async function remove(version: string | null): Promise<AppearanceSnapshot> {
    const signal = operations.current!.signal;
    try {
      await client.deleteAppearance(version, signal);
      const value = { version: null, details: emptyAppearance(), updatedAt: null, options: snapshot!.options };
      setSnapshot(value); return value;
    } catch (failure) {
      handleFailure(failure);
      if (failure instanceof ApiError && failure.uncertain && !signal.aborted) {
        try {
          const value = await client.appearance(signal);
          if (value.version === null) { setSnapshot(value); return value; }
        } catch (readFailure) { handleFailure(readFailure); if (readFailure instanceof StaleSessionError || (readFailure instanceof ApiError && readFailure.status === 401)) throw readFailure; }
        throw new ApiError(0, "We couldn't confirm deletion. Reload your personal details before continuing.", {}, true);
      }
      throw failure;
    }
  }
  return { snapshot, loadError, reload, save, remove };
}
