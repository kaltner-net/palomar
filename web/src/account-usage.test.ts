import { describe, expect, it } from "vitest";
import {
  accountUsageConstraint,
  accountUsageWindows,
  mergeAccountUsage,
  normalizeAccountUsage,
  rateLimitLabel,
} from "./account-usage";

describe("account usage normalization", () => {
  it("migrates legacy primary and secondary windows without inventing labels", () => {
    const usage = normalizeAccountUsage({ providers: { codex: {
      available: true,
      rateLimits: {
        primary: { usedPercent: 12, windowDurationMins: 300 },
        secondary: { usedPercent: 35 },
      },
    } } });
    const windows = accountUsageWindows(usage?.providers.codex);

    expect(windows.map(({ id }) => id)).toEqual(["primary", "secondary"]);
    expect(rateLimitLabel(windows[0])).toBe("5-hour limit");
    expect(rateLimitLabel(windows[1])).toBe("Usage limit");
  });

  it("retains all provider-defined windows and identifies the tightest one", () => {
    const usage = normalizeAccountUsage({ providers: { "claude-code": {
      available: true,
      rateLimits: { windows: [
        { id: "five_hour", label: "5-hour limit", usedPercent: 15, windowDurationMins: 300 },
        { id: "seven_day", label: "Weekly limit", usedPercent: 28, windowDurationMins: 10_080 },
        { id: "model_scoped:fable:1", label: "Fable weekly limit", usedPercent: 67, windowDurationMins: 10_080 },
        { id: "provider_period", label: "Provider period limit", usedPercent: 42 },
      ] },
    } } });
    const providerUsage = usage?.providers["claude-code"];

    expect(accountUsageWindows(providerUsage)).toHaveLength(4);
    expect(accountUsageConstraint(providerUsage)?.id).toBe("model_scoped:fable:1");
    expect(rateLimitLabel(accountUsageWindows(providerUsage)[3])).toBe("Provider period limit");
  });

  it("bounds public fields and excludes unrecognized providers and private payload data", () => {
    const usage = normalizeAccountUsage({ providers: {
      codex: { available: true, accountEmail: "private@example.com", rateLimits: { windows: [
        { id: "safe", label: "Safe\u0000 label", usedPercent: 140, resetsAt: 9e15 },
        { id: "bad id", usedPercent: 20 },
      ] } },
      private: { available: true, rateLimits: { windows: [{ id: "x", usedPercent: 10 }] } },
    } });

    expect(usage?.providers.codex?.rateLimits?.windows).toEqual([
      { id: "safe", label: "Safe  label", usedPercent: 100, resetsAt: 1_000_000_000_000 },
    ]);
    expect(usage?.providers).not.toHaveProperty("private");
    expect(JSON.stringify(usage)).not.toContain("private@example.com");
  });

  it("keeps provider caches isolated while replacing a freshly reported provider snapshot", () => {
    const previous = normalizeAccountUsage({ providers: {
      codex: { available: true, stale: true, rateLimits: { primary: { usedPercent: 10 } } },
      "claude-code": { available: true, rateLimits: { primary: { usedPercent: 20 } } },
    } });
    const merged = mergeAccountUsage(previous, { providers: {
      codex: { available: true, rateLimits: { windows: [{ id: "only", usedPercent: 30 }] } },
    } });

    expect(accountUsageWindows(merged?.providers.codex).map(({ id }) => id)).toEqual(["only"]);
    expect(merged?.providers.codex?.stale).toBeFalsy();
    expect(accountUsageWindows(merged?.providers["claude-code"])).toHaveLength(1);
  });

  it("does not erase the last valid snapshot when a reconnect reports usage unavailable", () => {
    const cached = normalizeAccountUsage({ providers: {
      codex: { available: true, rateLimits: { windows: [{ id: "known", usedPercent: 55 }] } },
    } });
    const merged = mergeAccountUsage(cached, { providers: {
      codex: { available: false, availabilityReason: "Temporarily unavailable" },
    } });

    expect(accountUsageWindows(merged?.providers.codex)).toHaveLength(1);
    expect(merged?.providers.codex?.stale).toBe(true);
  });
});
