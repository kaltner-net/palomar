# Palomar — self-hosted coding-agent control plane

<p align="center">
  <img src="docs/assets/palomar-wordmark.svg" alt="Palomar" width="360">
  <br>
  <strong>Self-hosted control plane for Codex and Claude Code on persistent Linux hosts.</strong>
</p>

Palomar is designed for persistent Linux coding hosts: an always-on VM, server,
homelab machine, or remote development box where Codex and Claude Code run. The
host is the durable execution environment; native Android and responsive web
clients are remote control surfaces for monitoring, steering, interrupting,
approving, resuming, and organizing its sessions.

Pair either client with one or more hosts and connect directly over a trusted
LAN or private overlay such as Tailscale or WireGuard. No Palomar-hosted
account, relay, or central backend is required.

Each host runs a small Palomar service that connects to the local Codex
app-server and to Claude Code through a bounded bridge built on the official
Claude Agent SDK. Provider sessions remain authoritative; Palomar supplies the
remote control plane around them.

> A Palomar host needs an authenticated Codex or Claude Code CLI. Claude Code
> additionally requires Node.js 20 or newer and the pinned Agent SDK.

See the [documentation](docs/README.md), [product roadmap](ROADMAP.md), and
[latest release](https://github.com/kaltner-net/palomar/releases/latest).

## Why Palomar?

Palomar supports a host-first workflow in which the controlling device never
becomes the coding-agent execution environment:

- **Persistent execution:** source trees, working directories, provider
  authentication, and provider session state stay on the Linux host. Work can
  continue when an Android or browser client disconnects.
- **Direct multi-host access:** pair Android and browser clients with multiple
  Linux hosts without routing session traffic through a hosted Palomar relay.
- **Provider-native behavior:** Codex and Claude Code sessions remain
  authoritative. Palomar exposes only the models, permissions, and live
  operations supported by the active provider.
- **Remote supervision:** follow live work and use provider-supported prompts,
  steering, interrupts, approvals, structured input, resume, search, archive,
  and organization controls from either client.
- **Operational continuity:** durable pairing, device revocation, usage and
  context visibility, privacy-safe notifications, and signed updates support
  hosts intended to run unattended. Linux updates can roll back after failed
  activation.

[Codex Remote Control](https://learn.chatgpt.com/docs/remote-connections) and
[Claude Code Remote Control](https://support.claude.com/en/articles/14554000-claude-code-power-user-tips)
are the first-party paths for their respective ecosystems. Palomar instead
provides one independently hosted Android/web control plane for both providers
and multiple Linux machines, reached over infrastructure you manage. It does
not use either provider's Remote Control transport.

## Quick start

### 1. Prepare a Linux host

You need Python 3.10 or newer, Bash, `curl`, OpenSSL, and a systemd user
session. Install and authenticate at least one supported provider CLI:
Codex (`codex`) or Claude Code (`claude`). Claude Code support additionally
needs Node.js 20 or newer.

Confirm at least one provider works locally:

```sh
codex --version
# or
claude --version
```

Palomar does not install or authenticate either provider.

### 2. Install the latest stable Palomar release

```sh
curl -fsSL https://raw.githubusercontent.com/kaltner-net/palomar/main/scripts/install-palomar.sh | sh
```

The rootless bootstrapper verifies the pinned release identity, signed checksum
manifest, Linux archive checksum, and archive layout before running the
release's installer. To inspect the bootstrapper first, use a source checkout,
or review platform requirements, follow the [installation guide](docs/install.md).

### 3. Verify and pair the web client

Verify the service, print its browser URL, and create a single-use pairing code
valid for ten minutes:

```sh
palomar status
palomar web
palomar pair
```

Open the printed URL and enter the code. When opening it from another device,
replace `localhost` with the Linux host's trusted-LAN or private-overlay name or
address. The bundled web client normally listens on port `8766`.

Palomar runs as an enabled systemd user service. On a headless or SSH-managed
host, enable lingering so the user service starts at boot and survives the last
logout. Enabling the unit alone does not keep the user service manager alive.

```sh
sudo loginctl enable-linger "$USER"
loginctl show-user "$USER" -p Linger
```

The second command should report `Linger=yes`. This one-time host setting may
require administrator privileges; the Palomar installer itself never invokes
`sudo`.

### 4. Add Android

Download the signed Android APK from the
[latest release](https://github.com/kaltner-net/palomar/releases/latest), sideload
it, and create a new `palomar pair` code for the phone. Each code pairs exactly
one client. Android connects directly to the host on port `8765` by default.

## How it works

```text
Android ── authenticated JSONL/TCP :8765 ─┐
                                          ├─ Palomar service on Linux host ─┬─ Codex app-server
Browser ── HTTP + authenticated WS :8766 ─┘                                 └─ Claude Agent SDK bridge
```

Both clients use the same protocol and provider-aware session model. Palomar
serves the web application and Android transport from one `palomar.service`;
there is no separate web server, hosted relay, or Palomar cloud service to
manage. See the
[architecture](docs/architecture.md), [protocol](docs/protocol.md), and
[user guide](docs/user-guide.md) for details.

## Security

> [!CAUTION]
> Palomar authenticates its clients but does not terminate TLS. Keep it on a
> trusted LAN or private overlay such as Tailscale or WireGuard, or place the
> web listener behind a trusted HTTPS reverse proxy. Never expose its ports
> directly to the public internet.

Pairing codes are short-lived and single-use. The service stores token hashes,
Android protects its token with Android Keystore, and browser tokens remain in
browser-local storage. A paired client can control every enabled provider on
its host, so treat both network access and client devices as sensitive.

Linux and Android updates require signed official artifacts and explicit
confirmation. Read the [security overview](docs/security.md) for the complete
deployment boundary and links to the bootstrap, server-update, and APK trust
models.

## Project status

Palomar 2.0.0 is a clean-break product identity. It does not migrate or reuse
pre-2.0 commands, services, packages, application data, or client storage.
Android is currently distributed by sideloading, and some provider capabilities
differ; the clients expose only operations supported by the active provider and
host.

- [Latest release](https://github.com/kaltner-net/palomar/releases/latest)
- [Compatibility policy](docs/compatibility.md)
- [Known limitations](docs/user-guide.md#known-limitations)
- [Issue tracker](https://github.com/kaltner-net/palomar/issues)
- [Product roadmap](ROADMAP.md)

## Documentation

Start with the [documentation index](docs/README.md). It links installation,
client usage, architecture, integrations, security, updates, compatibility,
release engineering, and historical acceptance records.

- [Documentation](docs/README.md)
- [License](LICENSE)
- [Copyright notice](NOTICE)
- [Third-party notices](THIRD_PARTY_NOTICES.md)

## License

Palomar's original source code and first-party binaries are available under
the [Apache License 2.0](LICENSE). Bundled third-party components remain subject
to their own licenses and terms; see
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

Palomar is an open-source project created by [Michael Kaltner](https://kaltner.net).
Copyright 2026 Michael Kaltner.
