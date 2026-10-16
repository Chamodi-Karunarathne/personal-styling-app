import { GROUP_KEYS } from "../profile/types";

export class ApiError extends Error {
  constructor(public readonly status: number, message: string,
      public readonly fieldErrors: Record<string, string> = {}, public readonly uncertain = false) {
    super(message); this.name = "ApiError";
  }
}
export class StaleSessionError extends ApiError {
  constructor() { super(0, "Your session changed. Reload your profile before continuing."); }
}
export function readableError(error: unknown): string {
  return error instanceof ApiError ? error.message : "Unable to connect. Please try again.";
}
export function record(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
export function unexpected(uncertain = false): ApiError {
  return new ApiError(0, "The server returned an unexpected response. Please try again.", {}, uncertain);
}
type CsrfToken = { headerName: string; token: string };
export type AccountScope = Readonly<{ accountId: number; generation: number }>;

/** One browser queue and CSRF cache for authentication and account-owned requests. */
export class SessionApiClient {
  private csrf: CsrfToken | null = null;
  private queue: Promise<unknown> = Promise.resolve();
  private generation = 0;
  private ownerId: number | null = null;
  constructor(private readonly baseUrl: string) {}

  enqueue<T>(work: () => Promise<T>, signal?: AbortSignal): Promise<T> {
    const result = this.queue.then(() => { signal?.throwIfAborted(); return work(); });
    this.queue = result.catch(() => undefined);
    return result;
  }
  invalidate(): number { this.generation++; this.ownerId = null; return this.generation; }
  epoch(): number { return this.generation; }
  adopt(accountId: number, generation: number) {
    if (generation !== this.generation) return;
    if (this.ownerId !== null && this.ownerId !== accountId) this.generation++;
    this.ownerId = accountId;
  }
  reject(generation: number) { if (generation === this.generation) this.invalidate(); }
  scope(accountId: number): AccountScope { return Object.freeze({ accountId, generation: this.generation }); }
  isCurrent(scope: AccountScope): boolean { return scope.generation === this.generation && scope.accountId === this.ownerId; }
  assert(scope: AccountScope) { if (!this.isCurrent(scope)) throw new StaleSessionError(); }
  clearCsrf() { this.csrf = null; }

  private url(path: string): string {
    try {
      const origin = new URL(this.baseUrl);
      if (!["http:", "https:"].includes(origin.protocol) || origin.username || origin.password
          || origin.search || origin.hash || (origin.pathname !== "/" && origin.pathname !== "")) throw new Error();
      return new URL(path, origin).toString();
    } catch { throw new ApiError(0, "The authentication server address is not configured correctly."); }
  }
  async request(path: string, init: RequestInit = {}): Promise<unknown> {
    const url = this.url(path);
    const write = init.method === "POST" || init.method === "PUT";
    let response: Response;
    try {
      const timeout = AbortSignal.timeout(15_000);
      response = await fetch(url, { ...init, credentials: "include", cache: "no-store",
        signal: init.signal ? AbortSignal.any([init.signal, timeout]) : timeout });
    } catch {
      init.signal?.throwIfAborted();
      throw new ApiError(0, "Unable to connect. Please try again.", {}, write);
    }
    let body: unknown = null;
    if (response.status !== 204) {
      try { body = await response.json(); } catch { /* Never expose response text. */ }
    }
    init.signal?.throwIfAborted();
    if (!response.ok) {
      const fields: Record<string, string> = {};
      const profile = path.startsWith("/api/profile/");
      if (response.status === 400 && record(body) && record(body.fieldErrors)) {
        for (const [key, message] of Object.entries(body.fieldErrors)) {
          const group = /^preferences\.([A-Za-z]+)(?:\[\d+\](?:\.\w+)?)?$/.exec(key)?.[1];
          const allowed = profile
            ? ["version", "intent", "lastVisitedStep", "preferences"].includes(key) || GROUP_KEYS.some((candidate) => candidate === group)
            : ["fullName", "email", "password"].includes(key);
          if (allowed && typeof message === "string") fields[group ? `preferences.${group}` : key] = message;
        }
      }
      const csrfFailure = response.status === 403 && record(body) && body.message === "Invalid or missing CSRF token";
      const message = response.status === 400 ? "Please check the highlighted fields."
        : response.status === 401 ? (path === "/api/auth/login" ? "Invalid email or password" : "Authentication is required")
        : response.status === 409 ? (profile ? "Your preferences changed elsewhere. Reload before saving again" : "Email is already registered")
        : csrfFailure ? "Invalid or missing CSRF token"
        : response.status === 403 ? "Access denied" : "Something went wrong. Please try again.";
      throw new ApiError(response.status, message, fields, write && response.status >= 500);
    }
    return body;
  }
  async bootstrap(signal?: AbortSignal): Promise<CsrfToken> {
    if (this.csrf) return this.csrf;
    const body = await this.request("/api/auth/csrf", { signal });
    if (!record(body) || body.headerName !== "X-XSRF-TOKEN" || typeof body.token !== "string" || !body.token) throw unexpected();
    this.csrf = { headerName: body.headerName, token: body.token };
    return this.csrf;
  }
  async write(path: string, method: "POST" | "PUT", input: unknown, signal?: AbortSignal,
      guard?: { check: () => void; verify: () => Promise<void> }): Promise<unknown> {
    for (let attempt = 0; attempt < 2; attempt++) {
      guard?.check();
      const csrf = await this.bootstrap(signal);
      // Check after bootstrap and on each retry; this callback must not queue itself.
      guard?.check();
      await guard?.verify();
      guard?.check();
      signal?.throwIfAborted();
      try {
        return await this.request(path, { method, signal,
          headers: { [csrf.headerName]: csrf.token, ...(input !== undefined ? { "Content-Type": "application/json" } : {}) },
          body: input === undefined ? undefined : JSON.stringify(input) });
      } catch (error) {
        if (!(error instanceof ApiError) || error.status !== 403 || error.message !== "Invalid or missing CSRF token" || attempt === 1) throw error;
        this.csrf = null;
      }
    }
    throw new ApiError(403, "Invalid or missing CSRF token");
  }
}
export const sessionApiClient = new SessionApiClient(process.env.NEXT_PUBLIC_API_BASE_URL ?? "");
