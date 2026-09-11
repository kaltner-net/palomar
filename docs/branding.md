# Palomar production identity

Palomar uses the original **Network Dome**: a canonical semicircular instrument
arc over three nodes joined by a control path. It can be read as an observatory
looking outward or infrastructure connecting persistent hosts. The identity is
independent and does not imply affiliation with Palomar Observatory or Caltech.

## Brand foundation

- Dark: `#171527`
- Lavender: `#CFC1FD`
- Cyan: `#62F9F8`

Use the full-color lavender/cyan mark on dark branded surfaces and the dark
monochrome mark on light surfaces. Do not ship a general-purpose white logo.
Android notification, themed-launcher, and system-status assets are technical
alpha masks whose visible color is supplied by the platform.

Use `currentColor` only in a component that intentionally owns the glyph color.
Do not stretch or redraw the semicircular geometry. Gradients, glow, texture,
shadows, and waves belong to presentation artwork, never the canonical mark.
Status and connectivity indicators remain separate semantic UI elements.

## Canonical repository assets

The maintainable masters live in [`docs/brand`](brand/). Runtime derivatives
must preserve their geometry and palette.

| Asset | Purpose |
| --- | --- |
| `palomar-mark.svg` | Canonical transparent full-color mark at 24 px and larger |
| `palomar-mark-16px.svg` | Manually pixel-fitted favicon and exact 16 px use |
| `palomar-mark-monochrome-dark.svg` | Dark mark on light surfaces |
| `palomar-mark-currentcolor.svg` | Deliberately UI-controlled monochrome glyph |
| `palomar-app-icon.svg` | Complete dark launcher/app tile |
| `palomar-wordmark.svg` | Horizontal wordmark for light documents |
| `palomar-lockup.svg` | Extended product lockup for light documents |
| `palomar-hero-wide.svg` | Presentation artwork for README and launch surfaces |
| `palomar-social-preview.svg` | Editable repository/social-preview master |
| `palomar-social-preview.png` | 1200×630 sharing and Open Graph derivative |

Do not substitute letter avatars or use the complete launcher tile where a
transparent in-product mark is appropriate. The web runtime copies only the
favicon, canonical mark, light-surface mark, app icon, and social preview.

Android uses `palomar_mark` and `palomar_mark_dark` for theme-appropriate
in-product surfaces, `palomar_launcher_foreground` for adaptive icons,
`palomar_launcher_monochrome_mask` for Android 13+ themed icons, and
`palomar_notification_mask` for status-bar notifications. API 26–32 adaptive
icons intentionally omit `<monochrome>`; API 33+ icons include it. The 108 dp
foreground and mask preserve the same 64-unit geometry at `(22,22)`, keeping
the mark inside circle, squircle, rounded-square, and themed-icon safe areas.

## Approved messaging

- “Persistent coding agents. Under your control.”
- “Monitor / Steer / Approve / Anywhere”
- “Self-hosted / Authenticated / Persistent / On your network”
- “LAN / Tailscale / WireGuard”

Palomar is a control plane; do not imply that it deploys software. Describe
the authenticated direct connection precisely instead of making an
unqualified “secure” claim.

Palomar is an open-source project created by
[Michael Kaltner](https://kaltner.net).
