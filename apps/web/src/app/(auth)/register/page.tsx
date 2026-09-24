"use client";

import Link from "next/link";
import { AuthField } from "@/components/auth/auth-field";
import { SocialAuthButtons } from "@/components/auth/social-auth-buttons";
import { Button } from "@/components/ui/button";

export default function RegisterPage() {
  return (
    <>
      <header className="mb-8">
        <h1 className="font-heading text-3xl leading-tight font-semibold tracking-tight sm:text-4xl">
          Create your account
        </h1>
        <p className="mt-3 text-sm leading-relaxed text-muted-foreground">
          Start your styling journey today
        </p>
      </header>

      <form
        aria-label="Create your account"
        className="space-y-5"
        onSubmit={(event) => {
          // UI only: keep Enter from submitting or navigating with form values.
          event.preventDefault();
        }}
      >
        <AuthField
          id="full-name"
          name="name"
          label="Full name"
          type="text"
          autoComplete="name"
          placeholder="Full name"
        />
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
          autoComplete="new-password"
          placeholder="Password"
        />
        <Button
          type="button"
          className="mt-2 h-12 w-full rounded-lg focus-visible:border-primary focus-visible:ring-primary/40"
        >
          Create Account
        </Button>
      </form>

      <SocialAuthButtons />

      <p className="mt-8 text-center text-sm leading-relaxed text-muted-foreground">
        Already have an account?{" "}
        <Link
          href="/login"
          prefetch={false}
          className="rounded-sm font-semibold text-primary underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary"
        >
          Sign in
        </Link>
      </p>
    </>
  );
}
