import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { UnifiedDashboard } from "./UnifiedDashboard";
import type { StoredHost } from "./storage";
import type { HostOverviewSnapshot } from "./unified";

const host = (id: string): StoredHost => ({
  id, displayName: `Host ${id}`, host: `${id}.local`, tcpPort: 8765, webPort: 8766,
  deviceToken: `token-${id}`, pairedAt: 1, lastConnectedAt: 1000, lastKnownStatus: "disconnected",
  runtimeMode: null, isDefault: id === "one",
});

describe("unified dashboard", () => {
  it("shows five hosts, marks cached data stale, and navigates attention with compound identity", () => {
    const hosts = ["one", "two", "three", "four", "five"].map(host);
    const snapshot: HostOverviewSnapshot = {
      hostId: "two", observedAt: 2000, connection: "disconnected", palomarVersion: "1", codexVersion: "2",
      runtimeMode: "fallback", runtimeConnected: true, active: 1, waiting: 1, failed: 0,
      oldestTurn: { hostId: "two", sessionId: "same", title: "Collision", startedAt: 100 },
      latestCompletion: null, latestActivity: 500,
      attention: [{ hostId: "two", sessionId: "same", approvalId: "apr", sessionTitle: "Collision", repository: "/work/repo", type: "approval", startedAt: 200 }],
    };
    const open = vi.fn();
    const reconnect = vi.fn();
    render(<UnifiedDashboard hosts={hosts} activeHostId="one" snapshots={new Map([["two", snapshot]])} onOpenHost={vi.fn()} onOpenSession={open} onReconnect={reconnect} onEdit={vi.fn()} onForget={vi.fn()} />);
    expect(screen.getAllByText(/Stale snapshot/)).toHaveLength(5);
    expect(screen.getByText("0/5")).toBeInTheDocument();
    expect(screen.getByText("Palomar-managed Codex runtime")).toBeInTheDocument();
    expect(document.querySelector(".unified-attention-list .provider-badge")).toHaveTextContent("Codex");
    expect(screen.queryByText("Fallback")).not.toBeInTheDocument();
    const attentionOpen = screen.getAllByRole("button", { name: "Open" }).at(-1)!;
    fireEvent.click(attentionOpen);
    expect(open).toHaveBeenCalledWith(expect.objectContaining({ hostId: "two", sessionId: "same" }));
    fireEvent.click(screen.getAllByRole("button", { name: "Reconnect" })[1]);
    expect(reconnect).toHaveBeenCalledWith("two");
  });

  it("opens a live attention item on its exact host and session", () => {
    const live: HostOverviewSnapshot = {
      hostId: "one", observedAt: Date.now(), connection: "connected", palomarVersion: "1", codexVersion: "2",
      runtimeMode: "shared", runtimeConnected: true, active: 0, waiting: 1, failed: 0,
      oldestTurn: null, latestCompletion: null, latestActivity: Date.now(),
      attention: [{ hostId: "one", sessionId: "same", sessionTitle: "Needs input", repository: "/repo", type: "input", startedAt: Date.now() }],
    };
    const open = vi.fn();
    render(<UnifiedDashboard hosts={[host("one")]} activeHostId="one" snapshots={new Map([["one", live]])} onOpenHost={vi.fn()} onOpenSession={open} onReconnect={vi.fn()} onEdit={vi.fn()} onForget={vi.fn()} />);
    fireEvent.click(screen.getAllByRole("button", { name: "Open" }).at(-1)!);
    expect(open).toHaveBeenCalledWith(expect.objectContaining({ hostId: "one", sessionId: "same", type: "input" }));
  });

  it("shows the active host as status and keeps host management actions quiet", () => {
    const live: HostOverviewSnapshot = {
      hostId: "one", observedAt: Date.now(), connection: "connected", palomarVersion: "1", codexVersion: "2",
      runtimeMode: "shared", runtimeConnected: true, active: 0, waiting: 0, failed: 0,
      oldestTurn: null, latestCompletion: null, latestActivity: Date.now(), attention: [],
    };
    const edit = vi.fn();
    const forget = vi.fn();
    render(<UnifiedDashboard hosts={[host("one")]} activeHostId="one" snapshots={new Map([["one", live]])} onOpenHost={vi.fn()} onOpenSession={vi.fn()} onReconnect={vi.fn()} onEdit={edit} onForget={forget} />);

    expect(screen.queryByRole("button", { name: "Current host" })).not.toBeInTheDocument();
    expect(screen.getByText("Current host")).toHaveClass("current-host-cue");
    fireEvent.click(screen.getByRole("button", { name: "Edit" }));
    fireEvent.click(screen.getByRole("button", { name: "Forget" }));
    expect(edit).toHaveBeenCalledWith("one");
    expect(forget).toHaveBeenCalledWith("one");
  });
});
