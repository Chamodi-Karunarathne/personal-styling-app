import Image from "next/image";
import type { ReactNode } from "react";

export default function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <main className="grid min-h-screen w-full flex-1 bg-card text-card-foreground md:grid-cols-2">
      <div className="flex min-w-0 items-center justify-center px-6 py-10 sm:px-12 sm:py-14 lg:px-16 lg:py-16">
        <div className="w-full max-w-sm">{children}</div>
      </div>
      <div className="relative hidden bg-muted md:block" aria-hidden="true">
        <Image
          src="/images/auth-fashion.webp"
          alt=""
          fill
          sizes="(min-width: 768px) 50vw, 1px"
          className="object-cover object-top"
        />
      </div>
    </main>
  );
}
