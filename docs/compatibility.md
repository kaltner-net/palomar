# Compatibility policy

This policy applies to Palomar 2.x beginning with `v2.0.0`. Palomar 2.0.0 is a
clean break: it does not recognize or migrate pre-2.0 CLI names, systemd units,
Android packages, environment variables, storage keys, installation paths, or
release artifacts.

## Protocol and clients

Protocol v1 is supported throughout the 2.x series. Additive protocol changes
must remain safe for older v1 clients to ignore. A change that makes existing
v1 clients or servers incompatible requires a new protocol version and a
documented migration; it will not ship as an unannounced 1.x change.

The supported browser baseline is the current stable Chrome and Firefox
release at the time each Palomar release is published. Edge uses the same
standard browser APIs and is best-effort unless a release record explicitly
promotes it to supported. Browser notifications additionally require HTTPS or
localhost and a certificate trusted by the client.

The Android app supports Android 6.0 (API 23) and later. Published APK upgrades
retain the application ID and signing key so Android can preserve encrypted
saved-host tokens. Android does not support installing an APK with a lower
version code over a newer one. Palomar's sideload updater also requires a
strictly newer stable SemVer and version code, exact package identity, the
current trusted signer, and the signed five-asset release contract. Signing-key
rotation requires an explicit intermediate compatibility release; automatic
same-version installation, downgrade, or unannounced signer migration is not
supported. See [`android-apk-updates.md`](android-apk-updates.md).

## Linux upgrades and state

Supported upgrades use either the shared updater described in
`docs/server-updates.md` or a verified tagged checkout/release archive with
`./install.sh`. The shared updater accepts only a strictly newer stable release
with the same protocol version. Protocol migration and signing-key rotation
require an explicit intermediate release; the updater never guesses across
either boundary. The installer and updater preserve
`~/.config/palomar/palomar.env` and `~/.local/state/palomar`, including paired
client state, while replacing the installed payload. Activation failure must
restore the previous payload, launcher, updater helper, and service units
without modifying those configuration or state directories.

Within one update protocol version, releases preserve the fixed external helper
CLI used by the enabled boot-recovery unit. Changing that contract requires an
intermediate release that installs a compatible recovery unit before a later
protocol version can use the new contract.

Palomar supports upgrades from the latest published 2.x release to the next
release. Larger version jumps should be staged through the intervening release
when its notes require a migration. Release notes must identify any migration,
configuration deprecation, or rollback restriction.

## Versioning and fixes

Documented features and security boundaries are supported behavior. Compatible
bug fixes ship as patch releases. New compatible functionality ships in a minor
release. Intentional breaking changes require a major release and an explicit
compatibility decision. Product boundaries listed under README **Known limitations** do
not constitute compatibility defects unless a release claims to remove them.
