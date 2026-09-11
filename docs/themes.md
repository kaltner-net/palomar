# Palomar themes

Appearance has two independent settings on web and Android:

- **Color mode** controls whether Palomar follows the system or always uses Light or Dark.
- **Theme** selects a named palette whose backgrounds, surfaces, text, controls, and accent roles are designed together.

Both clients use the same stable theme IDs and names:

| ID | Name | Intent |
| --- | --- | --- |
| `palomar` | Palomar | Dark violet, lavender, and cyan production default |
| `harbor` | Harbor | Ocean blue and blue-green |
| `grove` | Grove | Natural green and warm neutral |
| `ember` | Ember | Warm plum and clay |
| `dune` | Dune | Warm sand, amber, and earthy neutral |
| `slate` | Slate | Cool blue-gray and steady blue |
| `high-contrast` | High Contrast | Maximum separation for text, controls, borders, focus, and status cues |

Every theme has an explicit light and dark palette. Palomar dark mode uses
`#171527` as its application background, `#CFC1FD` as its primary accent, and
`#62F9F8` selectively for links, focus, connectivity, and secondary emphasis.
Its light mode uses a lavender-neutral background, white surfaces, `#171527`
text, and accessibility-adjusted deep-violet and dark-cyan controls. The logo
retains its canonical colors rather than inheriting theme colors.

Semantic success, working, attention, warning, failure, and full-access roles
remain consistent and separate from theme accents. Labels, icons, borders, and
selected-state marks supplement color throughout the UI.

High Contrast uses stronger surface boundaries, focus indicators, disabled-state separation, and AAA-level contrast targets for primary, muted, accent, and semantic foreground roles. It remains a named palette independent from System/Light/Dark color mode, so users can combine it with either forced mode or their current OS mode.

## Local persistence and migration

Appearance remains a client-local, host-scoped preference. Web stores version 2 appearance records under `palomar.appearance.v2.<host-id>`; Android stores appearance version 2 in the existing per-host preferences file. Forgetting a host deletes its appearance data with the rest of that host's local presentation state.

Legacy accents migrate deterministically on first load:

| Legacy accent | Theme |
| --- | --- |
| Purple | Palomar |
| Blue, Teal | Harbor |
| Green | Grove |
| Orange, Red, Pink | Ember |

Migration preserves color mode, activity detail, and repository grouping. Unknown or malformed values fall back to Palomar and cannot prevent startup. After migration, the legacy accent key is removed.

The former persisted default theme ID `foreman` is accepted only as an
appearance migration value. It is rewritten to `palomar` on load without
changing color mode, activity detail, repository grouping, host scope, or any
other preference. No other former product identifier is accepted as a
compatibility alias.
