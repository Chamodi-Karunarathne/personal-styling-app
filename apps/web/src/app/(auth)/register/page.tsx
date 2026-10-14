"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import { AuthLink } from "@/components/auth/auth-link";
import { AuthField } from "@/components/auth/auth-field";
import { useAuthTransition } from "@/components/auth/auth-shell";
import { useSession } from "@/components/auth/session-provider";
import { SocialAuthButtons } from "@/components/auth/social-auth-buttons";
import { Button } from "@/components/ui/button";
import { ApiError, readableError, type RegisterRequest } from "@/lib/api/auth-client";
import { validateRegister } from "@/lib/auth/validation";

export default function RegisterPage() {
  const { register } = useSession();
  const navigate = useAuthTransition();
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
    const name = ["fullName", "email", "password"].find((field) => errors[field]);
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

    const form = event.currentTarget;
    const data = new FormData(form);
    const input: RegisterRequest = {
      fullName: String(data.get("fullName") ?? ""),
      email: String(data.get("email") ?? "").trim(),
      password: String(data.get("password") ?? ""),
    };
    const errors = validateRegister(input);
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
      const succeeded = await register(input, controller.signal);
      if (!controller.signal.aborted && succeeded) navigate("/login");
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
          Create your account
        </h1>
        <p className="mt-3 text-sm leading-relaxed text-muted-foreground">
          Start your styling journey today
        </p>
      </header>

      <form
        aria-label="Create your account"
        className="space-y-5"
        noValidate
        aria-busy={isSubmitting}
        onSubmit={handleSubmit}
      >
        <AuthField
          id="full-name"
          name="fullName"
          label="Full name"
          type="text"
          autoComplete="name"
          placeholder="Full name"
          required
          disabled={isSubmitting}
          error={fieldErrors.fullName}
        />
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
          autoComplete="new-password"
          placeholder="Password"
          required
          disabled={isSubmitting}
          error={fieldErrors.password}
        />
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
          {isSubmitting ? "Creating account…" : "Create Account"}
        </Button>
      </form>

      <SocialAuthButtons />

      <p className="mt-8 text-center text-sm leading-relaxed text-muted-foreground">
        Already have an account?{" "}
        <AuthLink
          href="/login"
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
          Sign in
        </AuthLink>
      </p>
    </>
  );
}
