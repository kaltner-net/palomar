# Development workflow

This page contains source-checkout procedures for Foreman contributors and
maintainers. Normal Linux installation uses the signed release payload and
does not require Node, Gradle, Android Studio, or a Python package installation.

## Rebuild the web client

The committed `web/dist` assets ship with the Linux payload. Maintainers
updating `web/src` must use the pinned Node version and refresh those assets
before committing:

```sh
cd web
npm ci
npm test
npm run typecheck
npm run build
git status --short dist
```

CI rebuilds the SPA and fails if the committed assets are stale.

## Test the optional Claude bridge

Use Node 20 or newer and the committed lockfile:

```sh
cd linux/claude_bridge
npm ci --ignore-scripts
npm run build
npm test
```

Release and CI packaging repeat a production-only install and exclude bridge
test fixtures from the archive. The production entry point remains
`bridge.mjs`; release-archive installation never downloads dependencies. A
source checkout can let `install.sh` perform the same production-only `npm ci`
inside its staging payload.

After ordinary automated tests, maintainers with authenticated Claude access
can run the explicit disposable adapter and WebSocket proofs documented in
[`claude-code-integration.md`](claude-code-integration.md). These are opt-in
because they invoke live models and may incur usage.

## Refresh the vendored Python dependency

Refresh the committed dependency payload from the exact pin in
`requirements.txt` with:

```sh
PYTHON=/path/to/python-with-pip scripts/vendor_python_dependencies.sh
```

This preparation command may access the package index. `./install.sh` never
invokes it and remains offline.

Release preparation, artifact signing, acceptance evidence, and publication
are documented separately in the [release process](releasing.md).
