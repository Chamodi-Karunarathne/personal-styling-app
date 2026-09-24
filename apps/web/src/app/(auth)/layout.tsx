import type { ReactNode } from "react";
import { AuthShell } from "@/components/auth/auth-shell";

export default function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <AuthShell
      registerImageSrc="/images/auth-fashion.webp"
      loginImageSrc="/images/auth-loginimg.jpg"
    >
      {children}
    </AuthShell>
  );
}
