"use client";

import Link from "next/link";
import type { ComponentProps } from "react";
import { useAuthTransition, type AuthPath } from "./auth-shell";

type AuthLinkProps = Omit<ComponentProps<typeof Link>, "href" | "onNavigate"> & {
  href: AuthPath;
};

export function AuthLink({ href, ...props }: AuthLinkProps) {
  const navigate = useAuthTransition();

  return (
    <Link
      {...props}
      href={href}
      scroll={false}
      onNavigate={(event) => {
        // Only intercept same-tab navigation; modified clicks still work normally.
        event.preventDefault();
        navigate(href);
      }}
    />
  );
}
