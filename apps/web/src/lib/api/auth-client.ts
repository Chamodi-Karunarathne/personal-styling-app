import { ApiError, record, SessionApiClient, sessionApiClient, unexpected } from "./session-client";
export { ApiError, readableError } from "./session-client";

export type Account = { id: number; fullName: string; email: string };
export type LoginRequest = { email: string; password: string };
export type RegisterRequest = LoginRequest & { fullName: string };

export function account(value: unknown): Account {
  if (!record(value) || !Number.isSafeInteger(value.id) || typeof value.id !== "number"
      || value.id < 1 || typeof value.fullName !== "string" || typeof value.email !== "string") throw unexpected();
  return { id: value.id, fullName: value.fullName, email: value.email };
}

/** Spring owns authentication and all cookies; no credentials are persisted in JS. */
export class AuthClient {
  private readonly transport: SessionApiClient;
  constructor(baseUrl: string | SessionApiClient) {
    this.transport = typeof baseUrl === "string" ? new SessionApiClient(baseUrl) : baseUrl;
  }
  me(signal?: AbortSignal): Promise<Account> {
    const generation = this.transport.epoch();
    return this.transport.enqueue(async () => {
      try {
        const user = account(await this.transport.request("/api/auth/me", { signal }));
        this.transport.adopt(user.id, generation);
        return user;
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) this.transport.reject(generation);
        throw error;
      }
    }, signal);
  }
  register(input: RegisterRequest, signal?: AbortSignal): Promise<Account> {
    return this.transport.enqueue(async () => account(await this.transport.write("/api/auth/register", "POST", input, signal)), signal);
  }
  login(input: LoginRequest, signal?: AbortSignal): Promise<Account> {
    // Invalidate immediately, before queued work can run under a changed session.
    const generation = this.transport.invalidate();
    return this.transport.enqueue(async () => {
      try {
        const result = account(await this.transport.write("/api/auth/login", "POST", input, signal));
        this.transport.clearCsrf();
        try { await this.transport.bootstrap(signal); } catch { this.transport.clearCsrf(); }
        this.transport.adopt(result.id, generation);
        return result;
      } catch (error) { this.transport.clearCsrf(); throw error; }
    }, signal);
  }
  logout(): Promise<void> {
    this.transport.invalidate();
    return this.transport.enqueue(async () => {
      try { await this.transport.write("/api/auth/logout", "POST", undefined); }
      catch (error) { if (!(error instanceof ApiError) || error.status !== 401) throw error; }
      finally { this.transport.clearCsrf(); }
      try { await this.transport.bootstrap(); } catch { this.transport.clearCsrf(); }
    });
  }
}
export const authClient = new AuthClient(sessionApiClient);
