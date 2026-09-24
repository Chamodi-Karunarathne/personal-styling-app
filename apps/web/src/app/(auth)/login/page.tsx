"use client";

import Link from "next/link";
import { AuthField } from "@/components/auth/auth-field";
import { AuthLink } from "@/components/auth/auth-link";
import { SocialAuthButtons } from "@/components/auth/social-auth-buttons";
import { Button } from "@/components/ui/button";

export default function LoginPage() {
  return (
    <>
      <header className="mb-8">
        <h1 className="font-heading text-3xl leading-tight font-semibold tracking-tight sm:text-4xl">
          Welcome back
        </h1>
        <p className="mt-3 text-sm leading-relaxed text-muted-foreground">
          Sign in to your account
        </p>
      </header>

      <form
        aria-label="Sign in to your account"
        className="space-y-5"
        onSubmit={(event) => {
          // UI only: never submit credentials or navigate with form values.
          event.preventDefault();
        }}
      >
        <AuthField
          id="email"
          name="email"
          label="Email address"
          type="email"
          autoComplete="email"
          placeholder="Email address"
        />
        <AuthField
          id="password"
          name="password"
          label="Password"
          type="password"
          autoComplete="current-password"
          placeholder="Password"
        />
        <div className="text-right">
          <Link
            href="/forgot-password"
            prefetch={false}
            className="rounded-sm text-sm font-medium text-primary underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary"
          >
            Forgot password?
          </Link>
        </div>
        <Button
          type="button"
          className="mt-2 h-12 w-full rounded-lg focus-visible:border-primary focus-visible:ring-primary/40"
        >
          Sign In
        </Button>
      </form>

      <SocialAuthButtons />

      <p className="mt-8 text-center text-sm leading-relaxed text-muted-foreground">
        Don&apos;t have an account?{" "}
        <AuthLink
          href="/register"
          className="rounded-sm font-semibold text-primary underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary"
        >
          Sign up
        </AuthLink>
      </p>
    </>
  );
}
