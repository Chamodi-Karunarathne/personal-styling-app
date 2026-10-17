import { account } from "./auth-client";
import { ApiError, record, SessionApiClient, sessionApiClient, StaleSessionError, unexpected, type AccountScope } from "./session-client";
import { GROUP_KEYS, STEPS, type Preferences, type PreferenceCatalog, type PreferenceGroup, type ProfileSnapshot, type ProfileWrite } from "../profile/types";
import { parseAppearance } from "./appearance-contract";
import type { AppearanceSnapshot, AppearanceWrite } from "../appearance/types";

function codes(value: unknown): value is string[] {
  return Array.isArray(value) && value.every((code) => typeof code === "string") && new Set(value).size === value.length;
}
export function parseCatalog(value: unknown): PreferenceCatalog {
  if (!record(value) || !Array.isArray(value.groups) || value.groups.length !== GROUP_KEYS.length) throw unexpected();
  const groups: PreferenceGroup[] = value.groups.map((group: unknown) => {
    if (!record(group) || !GROUP_KEYS.some((key) => key === group.key) || typeof group.label !== "string"
        || group.allowsRequired !== (group.key === "coverage") || !Array.isArray(group.options) || group.options.length === 0) throw unexpected();
    const options = group.options.map((option: unknown) => {
      if (!record(option) || typeof option.code !== "string" || typeof option.label !== "string"
          || (option.swatch != null && (typeof option.swatch !== "string" || !/^#[\da-f]{6}$/i.test(option.swatch)))) throw unexpected();
      return { code: option.code, label: option.label, ...(typeof option.swatch === "string" ? { swatch: option.swatch } : {}) };
    });
    if (new Set(options.map((option) => option.code)).size !== options.length) throw unexpected();
    return { key: group.key as PreferenceGroup["key"], label: group.label, allowsRequired: group.allowsRequired as boolean, options };
  });
  if (new Set(groups.map((group) => group.key)).size !== GROUP_KEYS.length) throw unexpected();
  return { groups };
}
export function parseProfile(value: unknown, uncertain = false): ProfileSnapshot {
  const fail = () => { throw unexpected(uncertain); };
  if (!record(value) || !record(value.preferences)) return fail();
  if (!(value.version === null || (typeof value.version === "number" && Number.isSafeInteger(value.version) && value.version >= 0))
      || !["NOT_STARTED", "IN_PROGRESS", "SKIPPED", "COMPLETED"].includes(String(value.onboardingStatus))
      || !STEPS.some((step) => step.code !== "APPEARANCE" && step.code === value.lastVisitedStep)
      || (value.appearanceStepPending !== undefined && typeof value.appearanceStepPending !== "boolean")
      || (value.appearanceStepPending === true && value.lastVisitedStep !== "REVIEW")) return fail();
  const timestamp = (input: unknown) => input === null || (typeof input === "string" && Number.isFinite(Date.parse(input)));
  if (!timestamp(value.updatedAt) || !timestamp(value.completedAt)
      || (value.onboardingStatus === "NOT_STARTED") !== (value.version === null)
      || (value.onboardingStatus === "COMPLETED") !== (value.completedAt !== null)
      || (value.version === null) !== (value.updatedAt === null)) return fail();
  const preferences = {} as Preferences;
  for (const key of GROUP_KEYS) {
    const answer = value.preferences[key];
    if (key === "coverage") {
      if (answer === null) preferences.coverage = null;
      else if (Array.isArray(answer) && answer.every((choice: unknown) => record(choice) && typeof choice.code === "string"
          && (choice.strength === "PREFERENCE" || choice.strength === "REQUIRED"))) {
        preferences.coverage = answer.map((choice) => ({ code: choice.code, strength: choice.strength }));
        if (new Set(preferences.coverage.map((choice) => choice.code)).size !== preferences.coverage.length) return fail();
      } else return fail();
    } else if (answer === null || codes(answer)) preferences[key] = answer === null ? null : [...answer];
    else return fail();
  }
  return { version: value.version as number | null, onboardingStatus: value.onboardingStatus as ProfileSnapshot["onboardingStatus"],
    lastVisitedStep: value.lastVisitedStep as ProfileSnapshot["lastVisitedStep"], preferences,
    completedAt: value.completedAt as string | null, updatedAt: value.updatedAt as string | null, appearanceStepPending: value.appearanceStepPending === true };
}

export class BoundProfileClient {
  constructor(private readonly transport: SessionApiClient, public readonly scope: AccountScope) {}
  isCurrent() { return this.transport.isCurrent(this.scope); }
  appearance(signal?: AbortSignal): Promise<AppearanceSnapshot> {
    return this.transport.enqueue(async () => {
      await this.verifyOwner(signal);
      const body = await this.transport.request("/api/profile/appearance", { signal });
      this.transport.assert(this.scope); return parseAppearance(body);
    }, signal);
  }
  saveAppearance(input: AppearanceWrite, signal?: AbortSignal): Promise<AppearanceSnapshot> {
    const snapshot = structuredClone(input);
    return this.transport.enqueue(async () => {
      const body = await this.transport.write("/api/profile/appearance", "PUT", snapshot, signal, {
        check: () => this.transport.assert(this.scope), verify: () => this.verifyOwner(signal),
      });
      this.transport.assert(this.scope); return parseAppearance(body, true);
    }, signal);
  }
  deleteAppearance(version: string | null, signal?: AbortSignal): Promise<void> {
    return this.transport.enqueue(async () => {
      await this.transport.write("/api/profile/appearance", "DELETE", { version }, signal, {
        check: () => this.transport.assert(this.scope), verify: () => this.verifyOwner(signal),
      });
      this.transport.assert(this.scope);
    }, signal);
  }
  private async verifyOwner(signal?: AbortSignal) {
    this.transport.assert(this.scope);
    const owner = account(await this.transport.request("/api/auth/me", { signal }));
    this.transport.assert(this.scope);
    if (owner.id !== this.scope.accountId) {
      this.transport.invalidate();
      throw new StaleSessionError();
    }
  }
  options(signal?: AbortSignal): Promise<PreferenceCatalog> {
    return this.transport.enqueue(async () => {
      this.transport.assert(this.scope);
      const body = await this.transport.request("/api/profile/options", { signal });
      this.transport.assert(this.scope);
      return parseCatalog(body);
    }, signal);
  }
  me(signal?: AbortSignal): Promise<ProfileSnapshot> {
    return this.transport.enqueue(async () => {
      await this.verifyOwner(signal);
      const body = await this.transport.request("/api/profile/me", { signal });
      this.transport.assert(this.scope);
      return parseProfile(body);
    }, signal);
  }
  save(input: ProfileWrite, signal?: AbortSignal): Promise<ProfileSnapshot> {
    const snapshot = structuredClone(input);
    return this.transport.enqueue(async () => {
      this.transport.assert(this.scope);
      const body = await this.transport.write("/api/profile/me", "PUT", snapshot, signal, {
        check: () => this.transport.assert(this.scope),
        verify: () => this.verifyOwner(signal),
      });
      this.transport.assert(this.scope);
      return parseProfile(body, true);
    }, signal).catch((error: unknown) => {
      if (error instanceof ApiError && error.status === 401) this.transport.reject(this.scope.generation);
      throw error;
    });
  }
}
export class ProfileClient {
  constructor(private readonly transport: SessionApiClient) {}
  forAccount(accountId: number): BoundProfileClient {
    return new BoundProfileClient(this.transport, this.transport.scope(accountId));
  }
}
export const profileClient = new ProfileClient(sessionApiClient);
