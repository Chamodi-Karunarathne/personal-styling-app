"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { AuthField } from "@/components/auth/auth-field";
import { AuthLink } from "@/components/auth/auth-link";
import { useSession } from "@/components/auth/session-provider";
import { SocialAuthButtons } from "@/components/auth/social-auth-buttons";
import { Button } from "@/components/ui/button";
import { ApiError, readableError, type LoginRequest } from "@/lib/api/auth-client";
import { validateLogin } from "@/lib/auth/validation";

export default function LoginPage() {
  const router = useRouter();
  const { login, notice, clearNotice } = useSession();
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const submitting = useRef(false);
  const activeRequest = useRef<AbortController | null>(null);

  useEffect(() => () => activeRequest.current?.abort(), []);

  function focusInvalidField(
    form: HTMLFormElement,
    errors: Record<string, string>,
    signal?: AbortSignal,
  ) {
    const name = ["email", "password"].find((field) => errors[field]);
    if (!name) return;
    requestAnimationFrame(() => {
      if (signal?.aborted) return;
      const input = form.elements.namedItem(name);
      if (input instanceof HTMLInputElement) input.focus();
    });
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting.current) return;

    clearNotice();
    const form = event.currentTarget;
    const data = new FormData(form);
    const input: LoginRequest = {
      email: String(data.get("email") ?? "").trim(),
      password: String(data.get("password") ?? ""),
    };
    const errors = validateLogin(input);
    setFieldErrors(errors);
    setServerError(null);
    if (Object.keys(errors).length) {
      focusInvalidField(form, errors);
      return;
    }

    const controller = new AbortController();
    activeRequest.current = controller;
    submitting.current = true;
    setIsSubmitting(true);
    try {
      const succeeded = await login(input, controller.signal);
      if (!controller.signal.aborted && succeeded) router.replace("/dashboard");
    } catch (error) {
      if (controller.signal.aborted) return;
      if (error instanceof ApiError) {
        setFieldErrors(error.fieldErrors);
        focusInvalidField(form, error.fieldErrors, controller.signal);
      }
      setServerError(readableError(error));
    } finally {
      if (activeRequest.current === controller) activeRequest.current = null;
      submitting.current = false;
      if (!controller.signal.aborted) setIsSubmitting(false);
    }
  }

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

      {notice && (
        <p
          role="status"
          className="mb-5 text-sm leading-relaxed text-muted-foreground"
        >
          {notice}
        </p>
      )}

      <form
        aria-label="Sign in to your account"
        className="space-y-5"
        noValidate
        aria-busy={isSubmitting}
        onSubmit={handleSubmit}
      >
        <AuthField
          id="email"
          name="email"
          label="Email address"
          type="email"
          autoComplete="email"
          placeholder="Email address"
          required
          disabled={isSubmitting}
          error={fieldErrors.email}
        />
        <AuthField
          id="password"
          name="password"
          label="Password"
          type="password"
          autoComplete="current-password"
          placeholder="Password"
          required
          disabled={isSubmitting}
          error={fieldErrors.password}
        />
        <div className="text-right">
          <AuthLink
            href="/forgot-password"
            onClick={(event) => {
              if (
                event.button === 0 &&
                !event.metaKey &&
                !event.ctrlKey &&
                !event.shiftKey &&
                !event.altKey
              ) {
                activeRequest.current?.abort();
              }
            }}
            className="rounded-sm text-sm font-medium text-primary underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary"
          >
            Forgot password?
          </AuthLink>
        </div>
        {serverError && (
          <p role="alert" className="text-sm leading-relaxed text-destructive">
            {serverError}
          </p>
        )}
        <Button
          type="submit"
          disabled={isSubmitting}
          className="mt-2 h-12 w-full rounded-lg focus-visible:border-primary focus-visible:ring-primary/40"
        >
          {isSubmitting ? "Signing in…" : "Sign In"}
        </Button>
      </form>

      <SocialAuthButtons />

      <p className="mt-8 text-center text-sm leading-relaxed text-muted-foreground">
        Don&apos;t have an account?{" "}
        <AuthLink
          href="/register"
          onClick={(event) => {
            if (
              event.button === 0 &&
              !event.metaKey &&
              !event.ctrlKey &&
              !event.shiftKey &&
              !event.altKey
            ) {
              activeRequest.current?.abort();
            }
          }}
          className="rounded-sm font-semibold text-primary underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary"
        >
          Sign up
        </AuthLink>
      </p>
    </>
  );
}
