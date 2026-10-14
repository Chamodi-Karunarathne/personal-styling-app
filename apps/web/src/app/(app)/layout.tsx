import type { ReactNode } from "react";
import type { Metadata } from "next";
import { RequireAuth } from "@/components/auth/require-auth";

export const metadata: Metadata = { title: "Your account | Personal Styling" };

export default function AppLayout({ children }: { children: ReactNode }) {
  return <RequireAuth>{children}</RequireAuth>;
}
