# CoffeeGrams for Android — Testing

## The strategy in one paragraph

The brewing logic lives in `:core`, a pure Kotlin module with no Android on its
classpath, so the tests that prove the app is *correct* run headless in seconds
with no emulator. Everything above that — ViewModels, screens, adapters — is
tested at the level it actually fails at: JVM unit tests for state, Compose UI
tests for screens, and a short manual checklist on a physical device for the two
things that genuinely cannot be automated (Play Billing and Doze).

## Prerequisites

`JAVA_HOME` must point at a JDK 21. If `./gradlew` reports *"Unable to locate a
Java Runtime"*, that is the cause:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21   # add to ~/.zshrc
```

The Android SDK path lives in `local.properties`, which is **gitignored** and
machine-specific. If it is missing:

```bash
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
```

---

## The suites

### 1. `:core` — the correctness gate

```bash
./gradlew :core:test
```

Headless. No emulator, no device, no Android SDK involvement. **This is the suite
that matters most.** It holds the 49 cases ported from the iOS Swift Testing
suite, which are the *conformance spec* for the whole port: they were written
against the shipping iOS app, so passing them means identical inputs produce
identical ratios, timelines, and step transitions on both platforms.

If this suite is red, nothing else is worth running.

Coverage, as of M2 (2026-08-07) — 49 cases across 4 files:

| Area | What is asserted | Cases |
|---|---|---|
| `BrewCalculator` | Dose ↔ water ↔ ratio in all three solve directions, rounding, bounds | 11 |
| `BrewMethodProfile` | All six profile constants — the brewing reference table | 7 |
| `BrewTimelineBuilder` | Step order, durations, bloom multipliers, pour counts, totals, espresso window, cold-brew notify time | 11 |
| `BrewTimerEngine` | Step transitions, fast-forward by large deltas, pause/resume, overrun, completion | 20 |

`EspressoTarget` / `ColdBrewPlan` are exercised through `BrewTimelineBuilderTest` rather than
their own file, mirroring the iOS suite layout.

### 2. `:app` unit tests — ViewModels and adapters

```bash
./gradlew :app:testDebugUnitTest
```

JVM tests for the five ViewModels, using **Turbine** to assert on `StateFlow`
emissions and the in-memory port doubles for storage, clock, haptics, and
purchases. Land in M6.

### 3. Compose UI tests

```bash
./gradlew :app:connectedAndroidTest
```

Needs a running emulator or a connected device. Covers the five screens: the Pro
gate on the method picker, calculator input and output, guided-brew step
rendering, the brew log and its star rating. Also asserts TalkBack content
descriptions — a numeric readout with no label is a defect, not a nice-to-have.
Land in M7.

### 4. Build gates

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease   # warnings-as-errors, lintVital, R8
```

Release is the strict one: it compiles with `allWarningsAsErrors`, runs
`lintVital`, and shrinks with R8. `:core` compiles with warnings-as-errors in
*every* build type, not just release, because it is the correctness proof.

### Everything at once

```bash
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
```

This is what CI runs (`.github/workflows/ci.yml`), plus a grep asserting that
`core/src/` contains no `android.*` or `androidx.*` imports.

---

## What cannot be automated

Two areas fail only on real hardware. Both have bitten real apps in ways that
cost real money, so the manual pass is not optional.

### Play Billing — requires a physical device (M8)

**Google Play Billing cannot be tested on the Android Emulator.** Google's
testing documentation requires Android-powered *hardware* with the current Play
Store installed. Use **license testers** to make real-flow purchases that are
charged nothing, against a debug build whose package name matches the Play
Console app — this removes the publish loop, but not the device requirement.

| # | Check | Passes when |
|---|---|---|
| 1 | Buy Pro as a license tester | Pro methods unlock **and** the purchase is acknowledged — verify in Play Console → Orders |
| 2 | Uninstall → reinstall → Restore | Entitlement returns via `queryPurchasesAsync()` |
| 3 | Decline the payment instrument | Entitlement is **not** granted |
| 4 | Already-owned purchase attempt | Handled gracefully, no error state |

> ⚠️ **Acknowledgement is mandatory on Play, unlike StoreKit's `finish()`.** An
> unacknowledged purchase is **auto-refunded** — 3 days in production, minutes for
> test purchases. Getting this wrong silently refunds paying customers. Never ship
> a billing change without re-running check 1.

### Timer continuity and notifications (M9)

| # | Check | Passes when |
|---|---|---|
| 5 | Start a V60 brew, lock the screen 3 min, unlock | Step and elapsed time are **correct**, not frozen and not reset |
| 6 | Start a brew, swipe the app away | Behaviour is defined and the foreground-service notification is accurate |
| 7 | Leave a brew running into Doze | Step transitions still land |
| 8 | Rotate the device mid-brew | Timer survives configuration change |
| 9 | Take an incoming call mid-brew | Timer survives |
| 10 | Schedule a cold brew, wait 12–24 h | Notification arrives; **modest Doze drift is acceptable** and is not a bug |
| 11 | Deny the notification permission | App remains fully usable |

---

## Cross-platform parity check

Do this once on real devices before M12, in addition to the automated suites.

For each of the six brew methods: run a representative dose and ratio through
**both** the iOS app and the Android app side by side and diff the results, then
do the same for the full guided timeline — step order, per-step durations, and
total time. Any divergence is a port bug in `:core`.

The 49 conformance cases should catch this mechanically. The manual pass is there
because "should" is doing real work in that sentence.

---

## Definition of done for a milestone

- `./gradlew :core:test` green
- `./gradlew :app:testDebugUnitTest` green
- `./gradlew :app:connectedAndroidTest` green *(from M7)*
- Debug **and** Release build warning-free
- Qodo review findings driven to **zero**
