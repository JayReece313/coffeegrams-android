# CoffeeGrams for Android — Design

The brand is identical to iOS. The *interaction grammar* is Android's.

That distinction governs every decision here: same palette, same type feel, same
warmth — but Material 3 components, Android navigation, Android gestures. Do not
port SwiftUI layout idioms across; port the brand across.

The iOS source of truth is `DESIGN.md` in [`JayReece313/coffeegrams`](https://github.com/JayReece313/coffeegrams).

---

## Brand palette

Six semantic tokens, each with a light and a dark value, extracted from the iOS
asset catalog. **These hex values are the contract** — they are not to be
re-derived, eyeballed, or "improved" independently on Android.

| Token | Light | Dark | Role |
|---|---|---|---|
| `Background` | `#F4EADB` | `#1C140E` | App background — the Cream that carries the 60% |
| `Surface` | `#FBF4E9` | `#2A1F16` | Cards and raised surfaces |
| `TextPrimary` | `#3A2A1E` | `#F1E7D8` | Espresso Brown — headings, body, primary UI |
| `TextSecondary` | `#6B5647` | `#B9A897` | Medium Roast Taupe — tips, notes, low-priority labels |
| `Accent` | `#C6852E` | `#E0A34A` | Caramel — primary buttons, key accents |
| `TimerActive` | `#A96B1E` | `#E7B45C` | Deeper Gold — the running-timer numerals |

`TextSecondary` is a **desaturated step of the Espresso Brown, not a new hue.**
Treat it that way if it ever needs adjusting.

### The 60-30-10 rule

| Share | Role | Token |
|---|---|---|
| **60%** | Background, cards, readable surfaces | Cream (`Background`, `Surface`) |
| **30%** | Text and primary UI | Espresso Brown (`TextPrimary`) |
| **10%** | Actions and accents **only** | Caramel (`Accent`) |

The 10% is the rule that actually gets broken. Caramel is for primary buttons,
the active timer, and the one thing on screen that matters most. A screen with
three caramel elements competing is a bug.

---

## Material 3 mapping

M3 has more colour roles than CoffeeGrams has tokens, so most roles derive rather
than being authored. The mapping below is what M3 implements in
`app/src/main/kotlin/com/jrlabapps/coffeegrams/ui/theme/`:

| M3 role | Token |
|---|---|
| `background` / `surface` | `Background` |
| `surfaceContainer`, `surfaceContainerHigh` | `Surface` |
| `onBackground` / `onSurface` | `TextPrimary` |
| `onSurfaceVariant` | `TextSecondary` |
| `primary` | `Accent` |
| `onPrimary` | `Background` (cream on caramel) |
| `tertiary` | `TimerActive` |

**No Dynamic Color.** M3's wallpaper-derived theming is deliberately *not*
enabled. CoffeeGrams is a branded app whose identity must match the iOS build;
letting the palette follow the user's wallpaper would break parity for no gain.
This is a considered exception to the usual Android default, and should stay one.

---

## Type

The iOS app uses a rounded system face to keep numerals friendly. Android's
equivalent is the system font with a rounded numeral treatment — the type scale
is built in M3 as a Material 3 `Typography`, sized to match the iOS hierarchy
rather than copying M3's defaults verbatim.

Non-negotiables:

- **Dose and ratio numerals are the hero.** They are the reason the screen exists.
- **Respect the system font scale.** Users at 200% text size must still be able to
  read the timer. Never hard-code `sp` values that ignore scaling, and never
  disable font scaling to protect a layout — fix the layout.

---

## Motion and feedback

- When a brew phase counts down, the large clock numerals shift from
  **`TextPrimary` → `TimerActive`** as a peripheral "running" signal. This is the
  single most important piece of motion in the app.
- Haptics fire on step transitions, not on every tick.
- Animation is functional, never decorative. If it does not tell the user
  something, remove it.

---

## Android platform conventions

These are the places where Android must differ from iOS, and where copying the
iOS design would be wrong:

- **Edge-to-edge** is enforced from API 35. Every screen handles window insets
  properly; content must never sit under the status or navigation bar.
- **Predictive back** is opted into, with correct back handling per screen. A
  running brew timer must not be silently destroyed by a back gesture.
- **Adaptive icon** with foreground, background, and monochrome layers — the
  monochrome layer is what themed icons use, and omitting it looks broken on
  modern launchers. The current icon in `app/src/main/res/` is a **placeholder**
  until M3 vectorises the real mark.
- **TalkBack** labels on every interactive element and every numeric readout. A
  timer that reads as "3" instead of "3 minutes remaining" is a defect.
- **Touch targets** are 48dp minimum.

---

## Status

As of **2026-08-07**: the palette above is extracted and recorded, and the
placeholder adaptive icon uses the real Cream and Espresso Brown values. The
Compose `ColorScheme`, type scale, and vectorised logo are **M3** work — the
theme in `ui/theme/Theme.kt` is currently M3 defaults, not the brand palette.
