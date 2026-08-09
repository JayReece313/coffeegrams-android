# CoffeeGrams for Android

Kotlin + Jetpack Compose port of **CoffeeGrams: Brew Calculator** — dose-and-ratio calculators and guided brew timers for six coffee methods, plus a brew log. By **JR Labs LLC**.

The iOS original is live on the App Store and lives in a separate repo ([`JayReece313/coffeegrams`](https://github.com/JayReece313/coffeegrams)). **This port does not modify it.**

| | |
|---|---|
| **Status** | M3 complete — design system (palette, type, real brand mark) landed; persistence (M4) is next |
| **Target** | Google Play, v1.0, feature parity with iOS 1.1 |
| **Package** | `com.jrlabapps.coffeegrams` |
| **Min / Target / Compile SDK** | 26 (Android 8.0) / 36 (Android 16) / 37.1 |
| **Toolchain** | AGP 9.3.1 · Kotlin 2.4.10 · Gradle 9.7.0 · JDK 21 |
| **Monetization** | One non-consumable, `com.jrlabapps.coffeegrams.pro`, $4.99 |
| **Privacy** | No accounts, no ads, no analytics, no third-party SDKs — Data safety declares *no data collected* |

---

## How to work in here

Written for a fresh Claude Code session that has never seen this repo.

### How to start

Open a new Claude Code session pointed at this directory, then **read [`PLAN.md`](PLAN.md) first** — it is the entry-point document. It contains the full feasibility analysis, cost breakdown, Google Play requirements, and the M0–M13 milestone list that all work is tracked against.

Work **one milestone per session** (see Cost & context efficiency in `CLAUDE.md` at the `Apps/` level). Don't run one long multi-day session across milestones.

### The rules

- **Never push to `main`.** Every push goes to a **new, descriptively named branch**, then a PR into `main`.
- One branch **per unit of work**, not per commit — multiple commits may share a branch.
- Keep local `main` clean and matching the remote.
- **Only commit or push when asked.**
- A **Qodo review** runs on every push. Drive findings to **zero** before merging or calling a milestone done.
- Debug **and** Release must build warning-free (warnings-as-errors on Release) before a milestone is done.

### Where things live

| Path | Purpose | When to edit |
|---|---|---|
| `PLAN.md` | The port plan: costs, Play requirements, M0–M13 milestones | When scope, costs, or milestone definitions change |
| `README.md` | This file | When status, structure, or workflow changes |
| `core/` | **Pure Kotlin** brewing logic — no Android imports | Porting or changing brewing math, timelines, timer state machine |
| `app/` | Compose UI, ViewModels, Room, platform adapters | All UI and Android-specific work |
| `ARCHITECTURE.md` | Codebase map with Mermaid diagrams | When layers or data flow change |
| `DESIGN.md` | Palette, 60-30-10 rules, Material 3 mapping | When visual design changes |
| `testing.md` | Test strategy and how to run each suite | When suites are added or commands change |
| `Releases/submission_<version>.md` | As-built Play Store runbook | At each release |
| `gradle/libs.versions.toml` | Every dependency and SDK version, in one place | Adding or bumping any dependency — never hard-code a version in a module script |
| `.github/workflows/ci.yml` | Build + test on every push and PR | When a suite or build gate is added |

### The main workflow

**First, `JAVA_HOME` must point at a JDK 21** — there is no system Java on macOS.
If `./gradlew` says *"Unable to locate a Java Runtime"*, this is why:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21   # add this to ~/.zshrc
```

`local.properties` (gitignored) points at the SDK. If missing:
`echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties`

```bash
# Pure logic — runs headless, no emulator, no device. This is the correctness gate.
./gradlew :core:test

# App unit tests
./gradlew :app:testDebugUnitTest

# Instrumented / Compose UI tests (emulator or device)
./gradlew :app:connectedAndroidTest

# Release build — warnings-as-errors, lintVital, R8
./gradlew :app:assembleRelease
```

See [`testing.md`](testing.md) for what each suite covers and the manual
device checklist that Play Billing and Doze require.

### Invariants that must stay in sync

These are the cross-file consistency rules that break silently if ignored:

- **`core/` must never import anything Android.** It is the direct counterpart of the iOS `CoffeeGramsCore` package and must stay headless and CLI-testable. It applies `kotlin-jvm`, not `kotlin-android`, so the SDK is not even on its classpath — and **CI greps `core/src/` and fails the build** on any `android.*` / `androidx.*` import.
- **`compileSdk` (37.1) and `targetSdk` (36) differ on purpose.** AndroidX forces the compile level; `targetSdk` is the runtime behaviour we opted into and tested. Do not "fix" them to match. Both live in `gradle/libs.versions.toml` with the reasoning inline.
- **Brewing constants must match iOS exactly.** Ratios, bloom multipliers, steep times, pour counts, and shot windows come from `BrewMethodProfile` in the iOS repo. The 49 ported test cases are the conformance spec — if they pass, the port is faithful. Changing a constant means changing it on **both** platforms.
- **Brew method raw strings are a persistence contract.** `v60`, `chemex`, `french_press`, `aeropress`, `cold_brew`, `espresso` are stored in the database as strings. Never rename them.
- **The free-tier gate lives in one place** — `BrewMethod.isFreeTier` (French Press only). Widening the free tier is a one-line change there, not a UI condition.
- **The IAP product ID `com.jrlabapps.coffeegrams.pro` must match** across the code, the Play Console listing, and the submission runbook.
- **Play purchases must be acknowledged.** Unlike StoreKit's `finish()`, Play *auto-refunds* unacknowledged purchases. Never ship a billing change without re-verifying acknowledgement.
- **Screenshots assert on-screen strings.** Renaming UI copy must fail a screenshot test rather than silently stale the store listing — same discipline as the iOS `capture.sh`.

### Status / what's next

As of **2026-08-08**:

- ✅ Planning complete; plan approved.
- ✅ Repo created (public); local + GitHub kanban boards populated with M0–M13.
- ✅ D-U-N-S number already issued to JR Labs LLC — the usual 30-day lead time does not apply.
- ✅ **M0** — Google Play developer account registered as an **organization** under JR Labs LLC. That account type is what exempts this app from the 12-tester / 14-day closed-test gate.
- ✅ **M1** — Gradle scaffold, `:core` / `:app` module split, version catalog, CI, and the doc set (`ARCHITECTURE.md`, `DESIGN.md`, `testing.md`). All four gates green: `:core:test`, `:app:testDebugUnitTest`, debug build, release build.
- ✅ **M2** — all 12 `CoffeeGramsCore` Swift sources ported to pure Kotlin, plus all 49 test cases (`BrewCalculatorTest` 11, `BrewMethodProfileTest` 7, `BrewTimelineBuilderTest` 11, `BrewTimerEngineTest` 20). All four gates green, `:core` compiles with `allWarningsAsErrors`.
- ✅ **M3** — Material 3 `ColorScheme` (light + dark) and type scale from the 6 iOS color tokens, `BrewMethod`'s placeholder icon mapping ported, and the real adaptive icon + standalone logo mark transliterated from `coffeegrams_logo/render.swift` (replacing the M1 placeholder cup silhouette). All four gates green, including a real-device visual check of the launcher icon.
- ✅ **Play Small Business Program** — applied for and confirmed opted in (15% fee rate).
- ✅ **Physical Android test device acquired** — the M8 hardware blocker (Play Billing cannot be tested on the emulator) is cleared.
- ⬜ **Next: M4** — persistence: Room entity mirroring `BrewLogRecord`'s 11 columns, DAO, and the `BrewLogStoring` port + in-memory test double.
- ⬜ **Still to confirm in Play Console:** organization verification has cleared (the Small Business Program opt-in succeeding is a good sign, but this wasn't separately reconfirmed).

**Local emulator note:** `android-37.0` system images crash on boot on the primary dev Mac (M4 Max, macOS 26.3.1) — a genuine QEMU bug in the graphics-passthrough channel, unrelated to GPU backend flags. `system-images;android-34;google_apis;arm64-v8a` boots clean. Default to API 34 for local AVDs on this machine until this is confirmed fixed upstream.

**Boards:** [GitHub project](https://github.com/users/JayReece313/projects/3) · local board `coffeegrams-android` in `~/Documents/claude_code/kanban_board` (`npm start`, then open `http://localhost:4317`).
