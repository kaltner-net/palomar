# Palomar roadmap

This roadmap records Palomar's product priorities. GitHub Issues are the source
of truth for implementation status, acceptance criteria, and discussion; this
file provides an ordered, repository-visible index.

Priorities are ordered within each section. New ideas normally enter **Later**
until the current **Now** list is complete or a correctness or security issue
requires reprioritization.

Palomar's current supported boundary remains the one documented in
[Architecture](docs/architecture.md). Items marked **Architecture gate** are
approved product direction for design, not implementation commitments. Their
issues must first produce a reviewed architecture and security decision and
update the applicable boundary documentation before implementation starts.

## Now — v2.0.0 identity and packaging

The current product pass establishes Palomar 2.0.0 as the clean-break identity
for every client, host, installer, artifact, and repository surface. GitHub
Issues remain the source of truth for selecting the next implementation target.

## Next — select a product foundation

No foundation issue has been promoted for implementation yet. Choose from the
ordered **Later** list below unless a correctness or security issue requires
reprioritization.

## Later — product foundations

### Architecture gate: [#60 First-class project management](https://github.com/kaltner-net/palomar/issues/60)

Model projects as durable host-scoped workspaces, optionally backed by Git, and
support safe import, creation, cloning, management, migration, and new-session
selection. The design must resolve Palomar's current no-Git-write boundary
before clone or filesystem mutation is implemented.

### [#61 Session lifecycle management](https://github.com/kaltner-net/palomar/issues/61)

Add durable renaming, project reassignment, bulk organization, export, stable
links, and duplicate/fork workflows while keeping archive, hide, forget,
provider deletion, and local-file deletion explicitly distinct.

### Architecture gate: [#62 Multi-agent session orchestration](https://github.com/kaltner-net/palomar/issues/62)

Explore durable parent-child sessions, scoped tasks, isolated worktrees,
concurrency limits, progress, follow-up instructions, interruption, and result
collection. The design must resolve Palomar's current no-coordinator,
no-Git-write, and bounded-persistence boundaries before implementation.

### Architecture gate: [#63 OpenCode managed-session provider support](https://github.com/kaltner-net/palomar/issues/63)

Add OpenCode as an optional third provider through its documented server and
SDK contracts. Begin with an ownership and live-steering spike, then establish
provider-neutral service and protocol boundaries before implementing the
adapter and complete web/Android parity.

## Recently completed

- [x] Keep repository and workspace session groups in stable natural-alphabetic
  order while retaining activity ordering within each group
  ([issue #109](https://github.com/kaltner-net/palomar/issues/109)).
- [x] Complete theme-aware badge colors and standardize Palomar branding,
  including the Android notification icon
  ([PR #107](https://github.com/kaltner-net/palomar/pull/107)).
- [x] Show and retain every provider account-usage limit on web and Android
  ([issue #71](https://github.com/kaltner-net/palomar/issues/71)).
- [x] Show connected devices and support host-scoped access revocation on web
  and Android ([PR #106](https://github.com/kaltner-net/palomar/pull/106)).
- [x] Reconcile unavailable providers as disabled while retaining their
  diagnostic Settings entries
  ([PR #99](https://github.com/kaltner-net/palomar/pull/99),
  [PR #100](https://github.com/kaltner-net/palomar/pull/100)).
- [x] Add a signed, fail-closed one-command Linux bootstrap installer
  ([PR #97](https://github.com/kaltner-net/palomar/pull/97)).
- [x] Support Codex-only, Claude Code-only, and dual-provider installations
  ([PR #95](https://github.com/kaltner-net/palomar/pull/95)).
- [x] Remove persistent active-turn configuration helper text while retaining
  accessible route locks
  ([PR #96](https://github.com/kaltner-net/palomar/pull/96)).
- [x] Group routine completed nonzero command activity in Focused transcript
  mode while preserving exact expanded outcomes
  ([PR #92](https://github.com/kaltner-net/palomar/pull/92)).
- [x] Show transient success feedback on copy actions
  ([PR #89](https://github.com/kaltner-net/palomar/pull/89)).
- [x] Support signed Android APK self-update through Android's system installer
  ([PR #88](https://github.com/kaltner-net/palomar/pull/88)).
- [x] Add a shared recoverable Palomar server update mechanism
  ([PR #85](https://github.com/kaltner-net/palomar/pull/85)).
- [x] Show when a supported Palomar release is available without installing it
  ([PR #81](https://github.com/kaltner-net/palomar/pull/81)).
- [x] Replace accent selection with coherent themes
  ([PR #79](https://github.com/kaltner-net/palomar/pull/79)).
- [x] View and restore archived Codex sessions from filters
  ([PR #78](https://github.com/kaltner-net/palomar/pull/78)).
- [x] Complete restart-safe session activity restoration across Codex resume
  notifications ([PR #77](https://github.com/kaltner-net/palomar/pull/77)),
  building on the durable timestamp groundwork in PR #64.
- [x] Hide redundant repository metadata on grouped session cards
  ([PR #76](https://github.com/kaltner-net/palomar/pull/76)).
- [x] Add About information to web and Android
  ([PR #72](https://github.com/kaltner-net/palomar/pull/72)).
- [x] Fail closed when tagged release assets are incomplete
  ([PR #70](https://github.com/kaltner-net/palomar/pull/70)).
- [x] Hide provider UI when only one provider is enabled
  ([PR #68](https://github.com/kaltner-net/palomar/pull/68)).
- [x] Remember the last-opened session per host across navigation and relaunch
  ([PR #66](https://github.com/kaltner-net/palomar/pull/66)).
- [x] Add durable provider session timestamp storage and restoration groundwork
  ([PR #64](https://github.com/kaltner-net/palomar/pull/64)).
- [x] Consolidate Android monitoring, attention, and foreground outcomes into
  one user-visible notification
  ([PR #50](https://github.com/kaltner-net/palomar/pull/50)).
- [x] Restore pending approvals after navigation and reconnect
  ([PR #46](https://github.com/kaltner-net/palomar/pull/46)).
- [x] Make session configuration immutable while active and durable across
  reconnects and restarts
  ([PR #47](https://github.com/kaltner-net/palomar/pull/47)).
- [x] Handle newly created Codex sessions before their first prompt
  ([PR #48](https://github.com/kaltner-net/palomar/pull/48)).
- [x] Add explicit Android Home-to-Sessions navigation and preserve navigation
  origin
  ([PR #49](https://github.com/kaltner-net/palomar/pull/49)).
- [x] Upgrade Android Gradle Plugin to 9.3.2 and Gradle to 9.5.0
  ([PR #43](https://github.com/kaltner-net/palomar/pull/43)).

## Tracking convention

- GitHub Issues are the canonical task list and hold status, acceptance
  criteria, design discussion, and implementation links.
- This file communicates product order and larger outcomes by linking those
  issues; it does not maintain a second independent status record.
- GitHub Projects can provide board and timeline views once the issue backlog
  is large enough to benefit from structured status, priority, and dependency
  fields.
- Pull requests should link their issue and update this roadmap when they
  complete or materially change a listed outcome.
