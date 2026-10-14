import { expect, test } from "@playwright/test";
import { validateLogin, validateRegister } from "../../src/lib/auth/validation";

const input = { fullName: "Example User", email: "user@example.com", password: "ExamplePassword123" };

test("required fields, email format, and account field limits", () => {
  expect(validateRegister({ fullName: " ", email: "", password: " " })).toEqual({
    fullName: "Full name is required", email: "Email is required", password: "Password is required",
  });
  expect(validateRegister({ ...input, email: "invalid" }).email).toBe("Email must be valid");
  expect(validateRegister({ ...input, fullName: "a".repeat(151) }).fullName).toBeTruthy();
  expect(validateLogin({ ...input, email: "a".repeat(255) }).email).toBeTruthy();
  expect(validateRegister({ ...input, fullName: "a".repeat(150) }).fullName).toBeUndefined();
  // Do not introduce a domain-dot rule that the backend does not impose.
  expect(validateLogin({ ...input, email: "user@localhost" })).toEqual({});
});

test("registration keeps its eight-unit minimum; login does not impose it", () => {
  expect(validateRegister({ ...input, password: "1234567" }).password).toBe("Password must be at least 8 characters");
  expect(validateRegister({ ...input, password: "12345678" })).toEqual({});
  expect(validateLogin({ ...input, password: "x" })).toEqual({});
});

for (const [name, password] of [
  ["ASCII", "a".repeat(72)],
  ["mixed Unicode", "a".repeat(68) + "\u{1f600}"],
  ["Unicode", "\u{1f600}".repeat(18)],
] as const) {
  test(`${name} exactly at 72 UTF-8 bytes is accepted`, () => {
    expect(new TextEncoder().encode(password).byteLength).toBe(72);
    expect(validateRegister({ ...input, password })).toEqual({});
    expect(validateLogin({ ...input, password })).toEqual({});
  });
}

for (const [name, password] of [
  ["ASCII", "a".repeat(73)],
  ["mixed Unicode", "a".repeat(69) + "\u{1f600}"],
  ["Unicode", "\u{1f600}".repeat(19)],
] as const) {
  test(`${name} over the BCrypt byte boundary is rejected`, () => {
    expect(validateRegister({ ...input, password }).password).toBe("Password must be at most 72 UTF-8 bytes");
    expect(validateLogin({ ...input, password }).password).toBe("Password must be at most 72 UTF-8 bytes");
  });
}

test("validation never normalizes or changes submitted password contents", () => {
  const credentials = { ...input, password: "  CAFE\u0301-password  " };
  const original = credentials.password;
  expect(validateRegister(credentials)).toEqual({});
  expect(validateLogin(credentials)).toEqual({});
  expect(credentials.password).toBe(original);
});
