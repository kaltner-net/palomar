# Palomar themes

Appearance has two independent settings on web and Android:

- **Color mode** controls whether Palomar follows the system or always uses Light or Dark.
- **Theme** selects a named palette whose backgrounds, surfaces, text, controls, and accent roles are designed together.

Both clients use the same stable theme IDs and names:

| ID | Name | Intent |
| --- | --- | --- |
| `palomar` | Palomar | Ink/navy foundations, lavender structure, and cyan controls |
| `harbor` | Harbor | Ocean blue and blue-green |
| `grove` | Grove | Natural green and warm neutral |
| `ember` | Ember | Warm plum and clay |
| `dune` | Dune | Warm sand, amber, and earthy neutral |
| `slate` | Slate | Cool blue-gray and steady blue |
| `high-contrast` | High Contrast | Maximum separation for text, controls, borders, focus, and status cues |

Every theme has an explicit light and dark palette. Palomar follows the hero
artwork's hierarchy: neutral ink and navy carry the large surfaces, violet and
lavender provide branded structure and selected containers, and cyan marks
primary actions, links, focus, selected-item outlines, and live connectivity.
Usage meters retain lavender so cyan remains a recognizable interaction and
network accent rather than becoming a general-purpose decoration. The logo
retains its canonical colors rather than inheriting theme colors.

The production Palomar values are:

| Semantic role | Light | Dark |
| --- | --- | --- |
| Application background | `#F7F8FC` | `#090B16` |
| Primary surface | `#FFFFFF` | `#111326` |
| Alternate/grouped surface | `#EEF0F7` | `#171527` |
| Ordinary border/divider | `#D7DAE5` | `#30354D` |
| Primary text | `#111326` | `#FFFFFF` |
| Muted text | `#5D6175` | `#B6B7CA` |
| Interactive cyan | `#006E73` | `#62F9F8` |
| Brand structure | `#493B82` | `#CFC1FD` |
| Selected container | `#EAE5FF` | `#352D63` |
| Selected-container text | `#2F2853` | `#F5F0FF` |
| Disabled surface | `#E8EAF1` | `#202338` |
| Disabled text | `#686B7C` | `#989BAD` |
| Disabled border | `#CDD1DC` | `#30344A` |

Light-mode controls use `#006E73`, an accessibility-adjusted dark cyan, rather
than placing the hero's bright `#62F9F8` on white. Dark mode uses the canonical
bright cyan directly. The canonical structural violet and lavender values are
also used for repository hierarchy, product labels, selected containers, and
other restrained secondary emphasis.

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
