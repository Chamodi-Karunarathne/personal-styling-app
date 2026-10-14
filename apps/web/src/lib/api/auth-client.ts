export type Account = { id: number; fullName: string; email: string };
export type LoginRequest = { email: string; password: string };
export type RegisterRequest = LoginRequest & { fullName: string };
type CsrfToken = { headerName: string; token: string };

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
    public readonly fieldErrors: Record<string, string> = {},
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export function readableError(error: unknown): string {
  return error instanceof ApiError
    ? error.message
    : "Unable to connect. Please try again.";
}

function record(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function account(value: unknown): Account {
  if (!record(value) || !Number.isSafeInteger(value.id) || typeof value.id !== "number"
      || value.id < 1 || typeof value.fullName !== "string" || typeof value.email !== "string") {
    throw new ApiError(0, "The server returned an unexpected response. Please try again.");
  }
  // Only copy the public contract, even if a server accidentally adds fields.
  return { id: value.id, fullName: value.fullName, email: value.email };
}

/** Browser-only usage: Spring owns authentication and all cookies. */
export class AuthClient {
  private csrf: CsrfToken | null = null;
  private queue: Promise<unknown> = Promise.resolve();

  constructor(private readonly baseUrl: string) {}

  private enqueue<T>(work: () => Promise<T>, signal?: AbortSignal): Promise<T> {
    const result = this.queue.then(() => {
      signal?.throwIfAborted();
      return work();
    });
    this.queue = result.catch(() => undefined);
    return result;
  }

  private url(path: string): string {
    try {
      const origin = new URL(this.baseUrl);
      if (!["http:", "https:"].includes(origin.protocol) || origin.username || origin.password
          || origin.search || origin.hash || (origin.pathname !== "/" && origin.pathname !== "")) {
        throw new Error();
      }
      return new URL(`/api/auth/${path}`, origin).toString();
    } catch {
      throw new ApiError(0, "The authentication server address is not configured correctly.");
    }
  }

  private async request(path: string, init: RequestInit = {}): Promise<unknown> {
    const url = this.url(path);
    let response: Response;
    try {
      const timeout = AbortSignal.timeout(15_000);
      response = await fetch(url, {
        ...init,
        credentials: "include",
        cache: "no-store",
        signal: init.signal ? AbortSignal.any([init.signal, timeout]) : timeout,
      });
    } catch {
      init.signal?.throwIfAborted();
      throw new ApiError(0, "Unable to connect. Please try again.");
    }
    let body: unknown = null;
    if (response.status !== 204) {
      try { body = await response.json(); } catch { /* Never expose raw response text. */ }
    }
    init.signal?.throwIfAborted();
    if (!response.ok) {
      const fields: Record<string, string> = {};
      if (response.status === 400 && record(body) && record(body.fieldErrors)) {
        for (const key of ["fullName", "email", "password"]) {
          const message = body.fieldErrors[key];
          if (typeof message === "string") fields[key] = message;
        }
      }
      const csrfFailure = response.status === 403 && record(body)
        && body.message === "Invalid or missing CSRF token";
      const message = response.status === 400 ? "Please check the highlighted fields."
        : response.status === 401 ? (path === "login" ? "Invalid email or password" : "Authentication is required")
        : response.status === 409 ? "Email is already registered"
        : csrfFailure ? "Invalid or missing CSRF token"
        : response.status === 403 ? "Access denied"
        : "Something went wrong. Please try again.";
      throw new ApiError(response.status, message, fields);
    }
    return body;
  }

  private async bootstrap(signal?: AbortSignal): Promise<CsrfToken> {
    if (this.csrf) return this.csrf;
    const body = await this.request("csrf", { signal });
    if (!record(body) || body.headerName !== "X-XSRF-TOKEN" || typeof body.token !== "string" || !body.token) {
      throw new ApiError(0, "The server returned an unexpected response. Please try again.");
    }
    this.csrf = { headerName: body.headerName, token: body.token };
    return this.csrf;
  }

  private async post(path: string, input: LoginRequest | RegisterRequest | undefined, signal?: AbortSignal) {
    for (let attempt = 0; attempt < 2; attempt++) {
      const csrf = await this.bootstrap(signal);
      try {
        return await this.request(path, {
          method: "POST", signal,
          headers: {
            [csrf.headerName]: csrf.token,
            ...(input ? { "Content-Type": "application/json" } : {}),
          },
          body: input ? JSON.stringify(input) : undefined,
        });
      } catch (error) {
        if (!(error instanceof ApiError) || error.status !== 403
            || error.message !== "Invalid or missing CSRF token" || attempt === 1) throw error;
        this.csrf = null;
      }
    }
    throw new ApiError(403, "Invalid or missing CSRF token");
  }

  me(signal?: AbortSignal): Promise<Account> {
    return this.enqueue(async () => account(await this.request("me", { signal })), signal);
  }

  register(input: RegisterRequest, signal?: AbortSignal): Promise<Account> {
    return this.enqueue(async () => account(await this.post("register", input, signal)), signal);
  }

  login(input: LoginRequest, signal?: AbortSignal): Promise<Account> {
    return this.enqueue(async () => {
      try {
        const result = account(await this.post("login", input, signal));
        this.csrf = null;
        // Authentication already succeeded. A refresh outage must not report bad credentials.
        try { await this.bootstrap(signal); } catch { this.csrf = null; }
        return result;
      } catch (error) {
        this.csrf = null; // A lost response may still have changed cookies on the server.
        throw error;
      }
    }, signal);
  }

  logout(): Promise<void> {
    return this.enqueue(async () => {
      try {
        await this.post("logout", undefined);
      } catch (error) {
        if (!(error instanceof ApiError) || error.status !== 401) throw error;
        // An expired session is already signed out.
      } finally {
        this.csrf = null;
      }
      // Leave the browser ready for another sign-in. Failure is recoverable on its next POST.
      try { await this.bootstrap(); } catch { this.csrf = null; }
    });
  }
}

export const authClient = new AuthClient(process.env.NEXT_PUBLIC_API_BASE_URL ?? "");
