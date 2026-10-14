import type { LoginRequest, RegisterRequest } from "../api/auth-client";

function validateCredentials(input: LoginRequest): Record<string, string> {
  const errors: Record<string, string> = {};
  const email = input.email.trim();
  if (!email) errors.email = "Email is required";
  else if (email.length > 254) errors.email = "Email must be at most 254 characters";
  // Keep this initial check permissive; Jakarta validation remains authoritative.
  else if (!/^[^\s@]+@[^\s@]+$/.test(email)) errors.email = "Email must be valid";

  if (!input.password.trim()) errors.password = "Password is required";
  else if (new TextEncoder().encode(input.password).byteLength > 72) {
    errors.password = "Password must be at most 72 UTF-8 bytes";
  }
  return errors;
}

export function validateLogin(input: LoginRequest): Record<string, string> {
  return validateCredentials(input);
}

export function validateRegister(input: RegisterRequest): Record<string, string> {
  const errors = validateCredentials(input);
  if (!input.fullName.trim()) errors.fullName = "Full name is required";
  else if (input.fullName.length > 150) errors.fullName = "Full name must be at most 150 characters";
  if (!errors.password && input.password.length < 8) errors.password = "Password must be at least 8 characters";
  return errors;
}
