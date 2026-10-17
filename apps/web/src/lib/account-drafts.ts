/** Tab-lifetime memory only. No browser storage, credentials, or cross-account reuse. */
export class AccountDrafts {
  private owner: number | null = null;
  private readonly values = new Map<string, unknown>();
  verify(owner: number) { if (owner !== this.owner) { this.clear(); this.owner = owner; } }
  clear() { this.owner = null; this.values.clear(); }
  read<T>(owner: number | null, key: string): T | undefined {
    if (owner === null || owner !== this.owner || !this.values.has(key)) return undefined;
    return structuredClone(this.values.get(key)) as T;
  }
  remember<T>(owner: number | null, key: string, value: T | null) {
    if (owner === null || owner !== this.owner) return;
    if (value === null) this.values.delete(key); else this.values.set(key, structuredClone(value));
  }
  update<T>(owner: number, key: string, change: (value: T) => T) {
    const value = this.read<T>(owner, key);
    if (value !== undefined) this.remember(owner, key, change(value));
  }
}
