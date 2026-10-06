"use client";

import Link from "next/link";
import type { ComponentProps } from "react";
import { useAuthTransition, type AuthPath } from "./auth-shell";

type AuthLinkProps = Omit<ComponentProps<typeof Link>, "href" | "onNavigate"> & {
  href: AuthPath;
};

export function AuthLink({ href, className, ...props }: AuthLinkProps) {
  const navigate = useAuthTransition();

  return (
    <Link
      {...props}
      className={`${className ?? ""} dark:text-primary-foreground dark:focus-visible:outline-primary-foreground`}
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
