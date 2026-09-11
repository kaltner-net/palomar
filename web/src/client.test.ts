import { describe, expect, it, vi } from "vitest";
import { PalomarWebClient, inferPagePort, parseEndpoint } from "./client";

class MockSocket {
  readyState: number = WebSocket.CONNECTING;
  onopen: ((event: Event) => void) | null = null;
  onmessage: ((event: MessageEvent) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;
  onclose: ((event: CloseEvent) => void) | null = null;
  sent: Array<Record<string, unknown>> = [];
  hold = new Set<string>();
  rejectAuthentication = false;

  constructor() {
    queueMicrotask(() => {
      this.readyState = WebSocket.OPEN;
      this.onopen?.(new Event("open"));
    });
  }

  send(raw: string) {
    const request = JSON.parse(raw) as { id: string; type: string; payload: Record<string, unknown> };
    this.sent.push(request);
    if (this.hold.has(request.type)) return;
    const payload =
      request.type === "hello"
        ? { server: "Palomar", protocolVersion: 1, codexRuntime: "SHARED_DESKTOP_LIVE_STATUS_AVAILABLE", codexConnected: true, capabilities: {} }
        : request.type === "pair"
          ? { deviceToken: "pmt_browser" }
          : request.type === "authenticate"
            ? { authenticated: true }
            : { accepted: true };
    queueMicrotask(() =>
      this.onmessage?.(
        new MessageEvent("message", {
          data: JSON.stringify(request.type === "authenticate" && this.rejectAuthentication
            ? { version: 1, id: request.id, type: "error", payload: { code: "unauthorized", message: "device token is invalid" } }
            : { version: 1, id: request.id, type: `${request.type}.result`, payload }),
        }),
      ),
    );
  }

  close() {
    this.readyState = WebSocket.CLOSED;
  }

  drop() {
    this.readyState = WebSocket.CLOSED;
    this.onclose?.(new CloseEvent("close", { code: 1006 }));
  }

  revoke() {
    this.readyState = WebSocket.CLOSED;
    this.onclose?.(new CloseEvent("close", { code: 4003, reason: "Device token revoked" }));
  }
}

const tick = () => new Promise((resolve) => setTimeout(resolve, 0));

describe("web client pairing, authentication, and reconnect", () => {
  it("infers the Palomar web port from the page URL", () => {
    expect(inferPagePort("8766", "http:")).toBe(8766);
    expect(inferPagePort("9443", "https:")).toBe(9443);
    expect(inferPagePort("", "http:")).toBe(80);
    expect(inferPagePort("", "https:")).toBe(443);
  });

  it("parses hosts and derives secure WebSocket URLs without tokens", () => {
    expect(parseEndpoint("codex.local", "8766", "http:")).toEqual({
      host: "codex.local",
      port: 8766,
      httpUrl: "http://codex.local:8766",
      wsUrl: "ws://codex.local:8766/ws",
    });
    expect(parseEndpoint("https://[2001:db8::1]", 9443, "http:").wsUrl).toBe(
      "wss://[2001:db8::1]:9443/ws",
    );
    expect(() => parseEndpoint("host/path", 8766)).toThrow("without a path");
  });

  it("pairs, authenticates with the persistent token, and reports hello capabilities", async () => {
    const sockets: MockSocket[] = [];
    const onHello = vi.fn();
    const client = new PalomarWebClient(
      { onEvent: vi.fn(), onState: vi.fn(), onHello },
      () => {
        const socket = new MockSocket();
        sockets.push(socket);
        return socket;
      },
    );
    const endpoint = parseEndpoint("codex.local", 8766, "http:");
    await expect(client.pair(endpoint, "123456", "Browser")).resolves.toBe("pmt_browser");
    await client.start(endpoint, "pmt_browser", async () => undefined);
    expect(sockets.flatMap((socket) => socket.sent.map((message) => message.type))).toEqual([
      "hello",
      "pair",
      "hello",
      "authenticate",
    ]);
    expect(onHello).toHaveBeenCalled();
    client.disconnect();
  });

  it("re-authenticates and refreshes without replaying an interrupted prompt", async () => {
    const sockets: MockSocket[] = [];
    const timers: Array<() => void> = [];
    let readyCount = 0;
    const client = new PalomarWebClient(
      { onEvent: vi.fn(), onState: vi.fn() },
      () => {
        const socket = new MockSocket();
        sockets.push(socket);
        return socket;
      },
      (callback) => {
        timers.push(callback);
        return timers.length;
      },
    );
    const endpoint = parseEndpoint("codex.local", 8766, "http:");
    await client.start(endpoint, "pmt_browser", async () => {
      readyCount += 1;
    });
    sockets[0].hold.add("turn.prompt");
    const prompt = client.request("turn.prompt", { sessionId: "thread-1", text: "Only once" });
    sockets[0].drop();
    await expect(prompt).rejects.toThrow("Connection lost");
    expect(timers).toHaveLength(1);
    timers[0]();
    await tick();
    await tick();
    expect(readyCount).toBe(2);
    const sentTypes = sockets.flatMap((socket) => socket.sent.map((message) => message.type));
    expect(sentTypes.filter((type) => type === "turn.prompt")).toHaveLength(1);
    expect(sentTypes.filter((type) => type === "authenticate")).toHaveLength(2);
    client.disconnect();
  });

  it("tears down the old host and ignores its late events after switching", async () => {
    const sockets: MockSocket[] = [];
    const urls: string[] = [];
    const onEvent = vi.fn();
    const client = new PalomarWebClient(
      { onEvent, onState: vi.fn() },
      (url) => {
        urls.push(url);
        const socket = new MockSocket();
        sockets.push(socket);
        return socket;
      },
    );
    await client.start(parseEndpoint("home.local", 8766, "http:"), "pmt_home", async () => undefined);
    const oldSocket = sockets[0];
    await client.start(parseEndpoint("work.local", 9766, "http:"), "pmt_work", async () => undefined);

    oldSocket.onmessage?.(new MessageEvent("message", {
      data: JSON.stringify({ version: 1, type: "session.event", payload: { sessionId: "wrong-host" } }),
    }));

    expect(urls).toEqual(["ws://home.local:8766/ws", "ws://work.local:9766/ws"]);
    expect(onEvent).not.toHaveBeenCalled();
    expect(sockets[1].sent.find((message) => message.type === "authenticate")?.payload)
      .toEqual({ deviceToken: "pmt_work" });
    client.disconnect();
  });

  it("stops reconnecting and requests fresh pairing when its token is revoked", async () => {
    const sockets: MockSocket[] = [];
    const timers: Array<() => void> = [];
    const rejected = vi.fn();
    const onState = vi.fn();
    const client = new PalomarWebClient(
      { onEvent: vi.fn(), onState, onAuthenticationRejected: rejected },
      () => {
        const socket = new MockSocket();
        sockets.push(socket);
        return socket;
      },
      (callback) => {
        timers.push(callback);
        return timers.length;
      },
    );
    await client.start(parseEndpoint("codex.local", 8766, "http:"), "pmt_browser", async () => undefined);

    sockets[0].revoke();

    expect(rejected).toHaveBeenCalledWith(expect.stringContaining("token was revoked"));
    expect(onState).toHaveBeenLastCalledWith("disconnected", expect.stringContaining("token was revoked"));
    expect(timers).toHaveLength(0);
  });

  it("stops reconnecting and requests pairing when an offline token is rejected", async () => {
    const rejected = vi.fn();
    const onState = vi.fn();
    const timers: Array<() => void> = [];
    const client = new PalomarWebClient(
      { onEvent: vi.fn(), onState, onAuthenticationRejected: rejected },
      () => {
        const socket = new MockSocket();
        socket.rejectAuthentication = true;
        return socket;
      },
      (callback) => {
        timers.push(callback);
        return timers.length;
      },
    );

    await expect(client.start(
      parseEndpoint("codex.local", 8766, "http:"),
      "pmt_revoked",
      async () => undefined,
    )).rejects.toMatchObject({ code: "unauthorized" });

    expect(rejected).toHaveBeenCalledWith(expect.stringContaining("Pair this browser again"));
    expect(onState).toHaveBeenLastCalledWith("disconnected", expect.stringContaining("no longer valid"));
    expect(timers).toHaveLength(0);
  });
});
