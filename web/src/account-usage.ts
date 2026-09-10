import {
  isProviderId,
  type AccountUsage,
  type ProviderAccountUsage,
  type RateLimitSnapshot,
  type RateLimitWindow,
} from "./protocol";

const MAX_WINDOWS = 32;
const MAX_TIMESTAMP = 1_000_000_000_000;
export type NormalizedRateLimitWindow = RateLimitWindow & { id: string };

function safeText(value: unknown): string | undefined {
  if (typeof value !== "string") return undefined;
  const text = value.replace(/[\u0000-\u001f\u007f]/g, " ").trim().slice(0, 100);
  return text || undefined;
}

function safeId(value: unknown, fallback?: string): string | undefined {
  for (const candidate of [value, fallback]) {
    if (typeof candidate === "string" && /^[A-Za-z0-9._:-]{1,100}$/.test(candidate)) {
      return candidate;
    }
  }
  return undefined;
}

function boundedNumber(value: unknown, maximum: number): number | undefined {
  return typeof value === "number" && Number.isFinite(value) && value >= 0
    ? Math.min(Math.trunc(value), maximum)
    : undefined;
}

function normalizeWindow(value: unknown, fallbackId?: string): NormalizedRateLimitWindow | null {
  if (!value || typeof value !== "object") return null;
  const raw = value as Record<string, unknown>;
  if (typeof raw.usedPercent !== "number" || !Number.isFinite(raw.usedPercent)) return null;
  const id = safeId(raw.id, fallbackId);
  if (!id) return null;
  const duration = boundedNumber(raw.windowDurationMins, 525_600);
  const resetsAt = boundedNumber(raw.resetsAt, MAX_TIMESTAMP);
  return {
    id,
    usedPercent: Math.round(Math.max(0, Math.min(100, raw.usedPercent)) * 10) / 10,
    ...(safeText(raw.label) ? { label: safeText(raw.label) } : {}),
    ...(duration && duration > 0 ? { windowDurationMins: duration } : {}),
    ...(resetsAt !== undefined ? { resetsAt } : {}),
  };
}

export function normalizeRateLimitSnapshot(value: unknown): RateLimitSnapshot | undefined {
  if (!value || typeof value !== "object") return undefined;
  const raw = value as Record<string, unknown>;
  const candidates: Array<[unknown, string | undefined]> = [];
  if (Array.isArray(raw.windows)) {
    raw.windows.slice(0, MAX_WINDOWS).forEach((window) => candidates.push([window, undefined]));
  } else if (raw.windows && typeof raw.windows === "object") {
    Object.entries(raw.windows as Record<string, unknown>)
      .slice(0, MAX_WINDOWS)
      .forEach(([id, window]) => candidates.push([window, id]));
  }
  candidates.push([raw.primary, "primary"], [raw.secondary, "secondary"]);
  const windows: NormalizedRateLimitWindow[] = [];
  const seen = new Set<string>();
  for (const [candidate, fallbackId] of candidates) {
    if (windows.length >= MAX_WINDOWS) break;
    const window = normalizeWindow(candidate, fallbackId);
    if (!window || seen.has(window.id)) continue;
    seen.add(window.id);
    windows.push(window);
  }
  if (!windows.length) return undefined;
  const byId = new Map(windows.map((window) => [window.id, window]));
  const primary = normalizeWindow(raw.primary, "primary");
  const secondary = normalizeWindow(raw.secondary, "secondary");
  const metadata = Object.fromEntries(
    ["limitId", "limitName", "planType", "rateLimitReachedType"]
      .map((key) => [key, safeText(raw[key])])
      .filter((entry): entry is [string, string] => entry[1] !== undefined),
  );
  return {
    ...metadata,
    windows,
    ...(primary ? { primary: byId.get(primary.id) ?? primary } : {}),
    ...(secondary ? { secondary: byId.get(secondary.id) ?? secondary } : {}),
  };
}

function normalizeProviderUsage(value: unknown, stale: boolean): ProviderAccountUsage | undefined {
  if (!value || typeof value !== "object") return undefined;
  const raw = value as Record<string, unknown>;
  const rateLimits = normalizeRateLimitSnapshot(raw.rateLimits);
  const observedAt = boundedNumber(raw.observedAt, MAX_TIMESTAMP);
  return {
    available: rateLimits !== undefined || raw.available === true,
    ...(rateLimits ? { rateLimits } : {}),
    ...(raw.experimental === true ? { experimental: true } : {}),
    ...(observedAt !== undefined ? { observedAt } : {}),
    ...(safeText(raw.availabilityReason) ? { availabilityReason: safeText(raw.availabilityReason) } : {}),
    ...((stale || raw.stale === true) && rateLimits ? { stale: true } : {}),
  };
}

export function normalizeAccountUsage(value: unknown, stale = false): AccountUsage | null {
  if (!value || typeof value !== "object") return null;
  const rawProviders = (value as { providers?: unknown }).providers;
  if (!rawProviders || typeof rawProviders !== "object") return null;
  const providers: AccountUsage["providers"] = {};
  for (const [provider, rawUsage] of Object.entries(rawProviders)) {
    if (!isProviderId(provider)) continue;
    const usage = normalizeProviderUsage(rawUsage, stale);
    if (usage) providers[provider] = usage;
  }
  return { providers };
}

export function mergeAccountUsage(previous: AccountUsage | null, incoming: unknown): AccountUsage | null {
  const next = normalizeAccountUsage(incoming);
  if (!next) return previous;
  const providers = { ...(previous?.providers ?? {}) };
  for (const [provider, incomingUsage] of Object.entries(next.providers)) {
    if (!isProviderId(provider) || !incomingUsage) continue;
    const cached = providers[provider];
    if (!accountUsageWindows(incomingUsage).length && accountUsageWindows(cached).length) {
      providers[provider] = { ...cached!, stale: true };
    } else {
      providers[provider] = incomingUsage;
    }
  }
  return { providers };
}

export function accountUsageWindows(usage: ProviderAccountUsage | undefined): NormalizedRateLimitWindow[] {
  return (normalizeRateLimitSnapshot(usage?.rateLimits)?.windows ?? []) as NormalizedRateLimitWindow[];
}

export function accountUsageConstraint(usage: ProviderAccountUsage | undefined): NormalizedRateLimitWindow | null {
  const windows = accountUsageWindows(usage);
  return windows.reduce<NormalizedRateLimitWindow | null>(
    (constraint, window) => !constraint || window.usedPercent > constraint.usedPercent ? window : constraint,
    null,
  );
}

export function rateLimitLabel(window: RateLimitWindow): string {
  if (safeText(window.label)) return safeText(window.label)!;
  const durationMins = boundedNumber(window.windowDurationMins, 525_600);
  if (durationMins === 10_080) return "Weekly limit";
  if (durationMins && durationMins % 1_440 === 0) return `${durationMins / 1_440}-day limit`;
  if (durationMins && durationMins % 60 === 0) return `${durationMins / 60}-hour limit`;
  if (durationMins) return `${durationMins}-minute limit`;
  return "Usage limit";
}
