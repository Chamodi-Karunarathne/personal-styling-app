"use client";

import { ArrowLeftIcon } from "@phosphor-icons/react";
import { AuthField } from "@/components/auth/auth-field";
import { AuthLink } from "@/components/auth/auth-link";
import { Button } from "@/components/ui/button";

export default function ForgotPasswordPage() {
  return (
    <>
      <header className="mb-8">
        <h1 className="font-heading text-3xl leading-tight font-semibold tracking-tight sm:text-4xl">
          Forgot your password?
        </h1>
        <p className="mt-3 text-sm leading-relaxed text-muted-foreground">
          Enter the email address associated with your account and we&apos;ll send
          you a link to reset your password.
        </p>
      </header>

      <form
        aria-label="Reset your password"
        className="space-y-5"
        onSubmit={(event) => {
          // UI only: do not submit the email address or simulate a reset request.
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
        <Button
          type="button"
          className="mt-2 h-12 w-full rounded-lg focus-visible:border-primary focus-visible:ring-primary/40"
        >
          Send Reset Link
        </Button>
      </form>

      <div className="mt-8 text-center">
        <AuthLink
          href="/login"
          className="inline-flex items-center gap-2 rounded-sm text-sm font-semibold text-primary underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary"
        >
          <ArrowLeftIcon className="size-4" aria-hidden="true" />
          Back to sign in
        </AuthLink>
      </div>
    </>
  );
}
