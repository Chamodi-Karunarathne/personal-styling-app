"use client";

import { useCallback, useState } from "react";
import { useSession } from "@/components/auth/session-provider";

/** Each form decides which dirty/conflicted state to retain and when to discard it. */
export function useAccountDraft<T>(key: string) {
  const { session, drafts } = useSession();
  const owner = session.account?.id ?? null;
  const [initial] = useState(() => drafts.read<T>(owner, key));
  const remember = useCallback((value: T | null) => drafts.remember(owner, key, value), [drafts, owner, key]);
  const forget = useCallback(() => drafts.remember(owner, key, null), [drafts, owner, key]);
  return { initial, remember, forget };
}
