# CoffeeGrams for Android / Google Play — Feasibility & Port Plan

## Context

CoffeeGrams is live on the App Store (1.0 released 2026-07-29; 1.1 submitted 2026-08-04, awaiting review) as *CoffeeGrams: Brew Calculator* by JR Labs LLC. This plan answers a single question — **what would it take to ship the same app on Google Play**, and is it worth doing — then lays out the execution path if the answer is yes.

This would be the first Google Play submission. Goal is **parity**: same name, same six brew methods, same $4.99 Pro unlock, same privacy position ("Data Not Collected"), same visual identity.

**Decisions already made (2026-08-05):**
- **Play account:** Organization, under JR Labs LLC. **A D-U-N-S number is already issued and registered to JR Labs** — this removes what would otherwise have been the longest lead time in the project.
- **Codebase:** Native Kotlin + Jetpack Compose. The iOS repo is **not touched**.
- **Test hardware:** none currently owned — see the [device requirement](#the-one-hard-blocker-you-dont-own-yet) below, which revises this.
- **Repo:** new **public** repo `coffeegrams-android`, matching the existing `coffeegrams` / `coffeegrams-marketing` naming.

---

## Verdict: yes, this is worth doing — and it's an unusually cheap port

The architecture work already done on iOS pays off directly here. From the codebase inventory:

| Signal | Value | Why it matters |
|---|---|---|
| Pure-logic package (`CoffeeGramsCore`) | **1,073 lines**, imports only `Foundation` | Transliterates to Kotlin mechanically. Zero UI, zero Apple APIs. |
| Tests covering that logic | **49 Swift Testing cases**, 737 lines | Becomes a ready-made **conformance spec** — port the assertions, and the Kotlin engine is provably identical. |
| True platform adapters | **~320 lines total** (haptics 50, notifications 101, StoreKit 110, clock 20, diagnostics 39) | Already behind protocols with test doubles. The seams for Android implementations *already exist*. |
| Persisted entities | **1 table**, 11 columns | One Room entity. No migration history to honor. |
| IAP products | **1 non-consumable**, $4.99 | Simplest possible Play Billing integration. No subscriptions. |
| Third-party SDKs | **0** | Nothing to find an Android equivalent for. |
| Network calls | **0** (outside StoreKit) | No API, no backend, no auth. |
| Widgets / extensions / watch app | **none** | No secondary surfaces to rebuild. |
| Localizations | **English only**, 49 keys | One `strings.xml`. |

**What must genuinely be rewritten:** ~2,835 lines of SwiftUI/SwiftData/StoreKit app shell. Of that, ~1,500 lines are view code, ~470 are ViewModels that translate near-verbatim to Compose `ViewModel`s, and only ~320 are real platform glue.

There is **no annual fee** — the single biggest structural difference from Apple. See costs below.

**The honest counterweight:** this creates a second codebase in a second language that you'll maintain forever. That's a real, permanent tax. It's acceptable *here* specifically because the shared brewing logic is finished and stable — the ratios, timelines, and step tables in `BrewMethodProfile.swift` are reference data that won't churn. If the roadmap were volatile, the answer would be different.

---

## Costs — the direct answers

### Up front (one time)

| Item | Cost | Notes |
|---|---|---|
| Google Play developer registration | **$25, one time** | Paid once, covers the account forever. |
| D-U-N-S number (for the LLC) | **$0 — already obtained** | Already issued and registered to JR Labs. The 30-day lead time this normally carries does not apply. |
| Android Studio, Kotlin, Compose, JDK, Gradle, emulator | **Free** | All first-party, no licenses. |
| **Physical Android test device** | **~$80–200** | **Required** — see below. A used/budget phone is fine. |
| Privacy Policy + Support page hosting | **$0** | Already published at `docs/` on GitHub Pages. Reuse the same URLs. |
| **Total** | **~$105–225** | |

### Recurring

| Item | Cost |
|---|---|
| **Annual developer fee** | **$0 — there is none.** |

This is the headline difference from Apple. The Apple Developer Program is **$99/year, forever**. Google Play charges **$25 once**, with no renewal. Your ongoing cost to keep CoffeeGrams on Google Play is zero.

### Revenue share

Google takes a service fee on the $4.99 Pro unlock, same as Apple does:

- **15% on the first $1M/year** via the **Play Small Business Program** — opt in immediately; it's not automatic. 30% above $1M.
- Google is mid-rollout on a restructured US fee schedule (phasing in from June 2026 following the US antitrust remedies), which splits rates by new vs. existing installs and adds a separate billing-service component. The reporting on it is inconsistent and the official page is still being updated.
- **Practical planning number: assume ~15%,** effectively identical to Apple's Small Business Program. Confirm the exact schedule in Play Console at signup rather than trusting secondary sources. At CoffeeGrams' volume the difference is immaterial to the go/no-go decision.

### The one hard blocker you don't own yet

**Google Play Billing cannot be tested on the Android Emulator.** Google's official testing documentation requires *"any Android-powered hardware device... The most current version of the Google Play application must be installed."* Emulators are not a supported billing environment.

Everything else in this port — UI, timers, Room, notifications — tests fine on the emulator. But the **entire monetization path** does not. You cannot verify the paywall, purchase, acknowledgement, or restore flows without hardware.

**Recommendation: budget ~$100 for a used or budget Android phone before Milestone 8.** Any device running Android 8+ with the Play Store works; it does not need to be new or fast. This is not optional if the app ships with an IAP.

*Useful mitigation:* **license testers** let you sideload debug builds and make real-flow test purchases charged to nothing, **without publishing to a test track first** — as long as the package name matches the Play Console app. That removes the publish-loop friction, but not the device requirement.

---

## Approval process — how Play differs from Apple

You get one significant break and one significant new constraint.

**The break — you skip the 12-tester gate.** Google requires personal developer accounts created after 2023-11-13 to run a closed test with **12 opted-in testers for 14 continuous days**, then pass a production-access review, before *any* first release. This requirement is scoped to **personal** accounts. Registering as an **organization under JR Labs LLC** means it does not apply, and you can go straight to production. This alone is worth the D-U-N-S paperwork — it removes a 3+ week gate and the problem of sourcing 12 real testers.

> Verify this at signup. The exemption is consistently reported and follows from how Google scopes the policy, but Google's public page states the rule rather than enumerating exemptions. If the console tells you otherwise after verification, the fallback is the 12-tester closed test — plan for ~3–4 extra weeks.

**The remaining constraint — organization verification still takes some time, but far less.** With the D-U-N-S already issued, the 30-day issuance wait is gone; what's left is Google's own account verification. The one thing that reliably causes a bounce: your legal name and address in the **Google payments profile must exactly match the Dun & Bradstreet record**, character for character. Check that against the D&B profile *before* submitting, since a mismatch means a correction cycle through Google Payments Center. Register on day one and let verification run in parallel with M1–M2 development.

**Review itself is easier than Apple's.** Play review is largely automated with human spot-checks. First submissions from a new account often take **several days to a week**; subsequent updates are frequently hours. There's no equivalent of Apple's design-subjectivity rejections. Expect fewer rounds than the App Store.

**Release strategy:** publish to the **Internal testing** track first (up to 100 testers, no review delay, available in minutes) to validate the signed release build and the billing flow on your device. Then promote to Production with a **staged rollout** — Play lets you release to 20% of users and halt if crash rates spike, which has no App Store equivalent and is worth using.

**Required before you can submit** (Play's equivalents of Apple's checklist):
- **Data safety form** — Android's App Privacy label. Declares "no data collected," matching the iOS label.
- **Privacy policy URL** — mandatory for all apps. Reuse the existing GitHub Pages URL.
- **Content rating questionnaire** (IARC) — expect "Everyone."
- **Target audience & content**, **ads declaration** (none), **news app** (no), **financial features** (none), **government app** (no).
- **EU trader status** (DSA) — same declaration made for the App Store.
- **App access** — declare that all functionality is available without login (true; no accounts).

---

## Environment & dependencies

All free, all first-party. Your Mac is already a capable Android dev machine.

| Layer | Choice | Note |
|---|---|---|
| IDE | **Android Studio** (latest stable) | Includes SDK manager, emulator, Compose preview. |
| Language | **Kotlin 2.x** | |
| UI | **Jetpack Compose** + **Material 3** | The SwiftUI analogue. Declarative, same mental model. |
| Build | **Gradle** with version catalogs (`libs.versions.toml`) | |
| JDK | **JDK 17+** | Bundled with Android Studio. |
| Persistence | **Room** | The SwiftData analogue. |
| DI | **Manual constructor injection** | Matches the iOS approach — don't add Hilt for 5 ViewModels. |
| Billing | **`com.android.billingclient:billing-ktx`** | |
| Background work | **`androidx.work:work-runtime-ktx`** (WorkManager) | For the cold-brew notification. |
| Testing | **JUnit5 + kotlin.test** (logic), **Compose UI Test** (UI), **Turbine** (Flow) | |
| Screenshots | **Compose UI Test + `adb`** driving a script | Mirrors the existing `capture.sh` pattern. |

**Target/min SDK:** `targetSdk = 36` (Android 16) — mandatory for new apps submitted after **2026-08-31**, which is 26 days away, so build against it from the start. `minSdk = 26` (Android 8.0) covers effectively the whole active install base for an app this simple.

**Third-party dependency count: still zero.** Every library above is Google/JetBrains first-party (Turbine is the one small exception, test-only, never shipped). The "Data Not Collected" position survives intact.

---

## What is genuinely *new* engineering (not porting)

Four things Android does not give you for free that iOS did. These are the real risk, and where the time actually goes.

### 1. Timer continuity across backgrounding — the #1 risk
The iOS guided-brew timer is a **foreground-only 0.1s ticker** (`Timer.publish` + `.onReceive`) reading `systemUptime` deltas, with **no `scenePhase` recompute on restore**. iOS tolerates this. **Android will not** — it freezes tickers aggressively and Doze will stall a backgrounded app entirely.

A user starting a 4-minute V60 pour and locking their screen must get correct step transitions. Required:
- A **foreground service** (`FOREGROUND_SERVICE_MEDIA_PLAYBACK` or a specialized type) with an ongoing notification showing the current step, **or**
- Recompute elapsed time from `SystemClock.elapsedRealtime()` on every resume and fast-forward `BrewTimerEngine.advance(by:)` to catch up.

**Do both.** The good news: `BrewTimerEngine` is already a **pure delta-driven state machine** (`advance(by:)`), never reading a clock itself. Fast-forwarding it by a large delta is exactly what it was designed for. Verify against `BrewTimerEngineTests`' 20 cases.

### 2. Notification permission & delivery
- `POST_NOTIFICATIONS` is a **runtime permission** on API 33+. iOS's `requestAuthorization` maps to it, but the UX must handle denial gracefully (the app must still work).
- The **cold brew notification fires 12–24 hours out**. Doze can delay inexact alarms. Use WorkManager or `setAndAllowWhileIdle`; avoid `SCHEDULE_EXACT_ALARM`, which is heavily restricted and would invite Play policy questions for a non-alarm-clock app. A few minutes' drift on a 12-hour cold brew is fine — do not fight for exactness.

### 3. Play Billing semantics differ from StoreKit 2
- **No `AppStore.sync()` analogue.** "Restore Purchase" becomes `queryPurchasesAsync()`. The button stays; the implementation is simpler.
- **Purchases must be acknowledged or they auto-refund** (3 days in production; minutes for test purchases). StoreKit's `transaction.finish()` is best-practice; Play's `acknowledgePurchase` is **mandatory**. Getting this wrong silently refunds real customers.
- `PurchasesUpdatedListener` + `queryPurchasesAsync` map cleanly onto the existing `entitlementUpdates()` / `isPurchased()` port, so `PurchaseController` and its gate (`canAccess(_:)`) port unchanged.

### 4. Platform UI conventions
- **Edge-to-edge is enforced** at API 35+. Compose scaffolding must handle window insets properly.
- **Predictive back** gestures need opting in and correct back handling.
- **Adaptive icons** (foreground/background layers) must be regenerated — `coffeegrams_logo/render.swift` produces a flat PNG. Re-render or vectorize to a Compose `ImageVector` / VectorDrawable.
- **Material 3 vs. HIG.** Apply the existing 60-30-10 palette (Cream / Espresso Brown / Caramel) to an M3 `ColorScheme` rather than importing iOS layout idioms. The brand should be identical; the *interaction grammar* should be Android's.

---

## Repository & tracking

Per the repo standards, this gets **its own new repo** — `CoffeeGramsAndroid` — set up like CoffeeGrams: branch-per-unit-of-work, PR for every change, Qodo review on every push driven to zero findings, never push to `main`.

Ships with the required docs from the start, not as afterthoughts:
- `README.md` (incl. the **`## How to work in here`** section), `ARCHITECTURE.md` (Mermaid layer/flow/test diagrams), `DESIGN.md` (palette + M3 mapping, cross-referencing the iOS `DESIGN.md`), `testing.md`, and `Releases/submission_1.0.md` as the as-built Play runbook.

A **GitHub Projects board** under `JayReece313` mirrors the milestones below — every milestone and deliverable gets a card with **both a column and a one-line description**, moved to in-progress on start and done on completion.

### How releases map between the two platforms

Decided 2026-08-13, once iOS had shipped past this port's original baseline
(1.1 live, 1.2 — iPad + rating prompt — in planning) while Android was still
mid-port. Written down so a later session doesn't try to invent a lockstep
versioning scheme neither platform needs.

**Version numbers do not stay in sync across platforms.** iOS and Android are
separate binaries in separate stores with separate review clocks — "Android
1.2 ships alongside iOS 1.2" is coordination overhead with no user-facing
payoff; nobody compares version numbers across the App Store and Play
listings side by side.

**What has to stay in sync is decisions, not release cadence.** The NO-ads
call, the $4.99 one-time Pro price, the six brew methods, and the core
brew-calculation/timer logic are the actual conformance spec — `:core` must
keep matching `CoffeeGramsCore` exactly, per the porting standard in the root
`CLAUDE.md`. Those aren't re-decided per platform or per release.

**Port feature-by-feature when an iOS release ships, not release-by-release:**
- **Core logic changes** (a new `BrewMethod`, a ratio-calc fix) → always port
  mechanically into `:core`, same as the initial conformance-spec port.
- **iOS-idiomatic features with a real Android equivalent** → port the
  *intent*, not the API. Example: iOS 1.2's SwiftUI `requestReview` rating
  prompt maps to Android's own **Play In-App Review API** — same product
  goal, different platform call — and it lands in whatever Android version
  is current when it's actually built, not "Android 1.2."
- **Platform-specific presentation work** → no automatic obligation. iOS
  1.2's iPad + `NavigationSplitView` layout pass doesn't map 1:1: Compose's
  `WindowSizeClass`/adaptive layouts already cover more of that ground by
  default, and Android's tablet share is smaller than iPad's role on iOS. If
  a large-screen pass is ever worth doing on Android, it's its own
  separately-scoped task, evaluated on its own merits — not something
  triggered automatically by iOS shipping iPad support.

**Concretely:** this port's own **1.0 targets parity with the iOS feature set
the port started from** (~1.1 — method list, guided timers, brew log with
planned/actual timing) — not iOS's in-flight 1.2/1.3 work. Once this ships,
each subsequent iOS release becomes a set of individually-evaluated
candidates for *this* repo's own roadmap (a `Releases/roadmap_future.md`
equivalent, once one exists here) — referencing the iOS roadmap for shared
*decisions*, never for version numbers.

---

## Milestones

Sequenced so the calendar-bound account work runs in parallel with code.

| # | Milestone | Deliverable |
|---|---|---|
| **M0** | **Play account registration** — *start day one, runs in parallel* | Play org account registered under JR Labs LLC ($25) using the **existing D-U-N-S**; payments profile name/address verified to match the D&B record exactly; Small Business Program opt-in; confirm the closed-testing exemption applies. |
| **M1** | Repo & toolchain | New repo, Gradle scaffold, `targetSdk 36`/`minSdk 26`, version catalog, module split (`:core` pure Kotlin, `:app` Android), CI, doc skeletons, Projects board populated. |
| **M2** | **Port `:core`** | All 12 source files transliterated: `BrewMethod` (incl. `isFreeTier`), `BrewMethodProfile` (all 6 profile constants — the brewing reference table), `BrewStep`, `BrewTimeline` + `BrewTimelineBuilder`, `BrewCalculator`, `BrewTimerEngine`, `EspressoTarget`, `ColdBrew`, `BrewLogEntry`, `MonotonicClock` port. **Plus all 49 test cases** — this module must be green and runnable from the CLI before any UI exists. |
| **M3** | Design system | 7 color tokens → M3 `ColorScheme` (light + dark), rounded type scale, `BrewMethod`/`BrewStep` presentation mappings, adaptive icon + vector logo regenerated. |
| **M4** | Persistence | Room entity mirroring `BrewLogRecord`'s 11 columns (method stored as its stable raw string), DAO, and the `BrewLogStoring` port + in-memory test double. |
| **M5** | Platform adapters | `SystemClock` (`elapsedRealtime`), haptics (`VibratorManager`/`HapticFeedbackConstants`), notifications (channel, `POST_NOTIFICATIONS`, WorkManager scheduling) behind the existing port shapes. Drop `DiagnosticsService` entirely. |
| **M6** | ViewModels | `CalculatorViewModel`, `GuidedBrewViewModel`, `EspressoShotViewModel`, `ColdBrewViewModel`, `PurchaseController` — near-verbatim from Swift, `StateFlow` in place of `@Observable`. Ported unit tests. |
| **M7** | Compose UI | The 5 screens: method picker (with Pro gate), calculator, guided brew, espresso shot, cold brew plan, brew log + detail + star rating. Edge-to-edge, predictive back, Dynamic Type equivalent, TalkBack labels. |
| **M8** | **Play Billing** — *needs the physical device* | `com.jrlabapps.coffeegrams.pro` created in Play Console, BillingClient integration, **acknowledgement**, restore via `queryPurchasesAsync`, paywall screen, license-tester verification of buy / decline / restore / already-owned. |
| **M9** | **Background timer hardening** | Foreground service + `elapsedRealtime` resume-recompute. Test: screen off mid-brew, app swiped away, Doze, device rotation, incoming call. |
| **M10** | Test suites & screenshots | Full unit + Compose UI suites green; screenshot harness (Compose UI Test + `adb`) producing Play-spec listing images with on-screen string assertions, mirroring `capture.sh`. |
| **M11** | Store listing & compliance | Title (≤30 chars — *"CoffeeGrams: Brew Calculator"* is 28, fits), short (≤80) + full (≤4000) descriptions, 512×512 icon, 1024×500 feature graphic, phone screenshots, Data safety form, content rating, trader status, privacy/support URLs. |
| **M12** | Release | Upload keystore generated **and backed up** (losing it is unrecoverable), Play App Signing enrolled, AAB built, Internal testing → validate on device → Production with staged rollout. `Releases/submission_1.0.md` written as-built. |
| **M13** | Retrospective | `CoffeeGramsAndroid_Summary.md` in the private `Summary` repo, plus a copy of `ARCHITECTURE.md`, including the standing **"where could AI agents help?"** process review. |

**With the D-U-N-S already in hand, the critical path moves back to the code** — specifically M2 (core port) → M7 (UI) → M9 (background timer). M0 still starts on day one so account verification clears well before M8 needs a live Play Console listing to test billing against.

---

## Verification

**Per-milestone gates (nothing is "done" until these pass):**
- `:core` — `./gradlew :core:test` green from the CLI, no emulator. All 49 ported cases pass. **This is the correctness proof for the entire port**: identical inputs must produce identical ratios, timelines, and step transitions to the shipping iOS app.
- App layer — `./gradlew :app:testDebugUnitTest` + `connectedAndroidTest` green; Debug **and** Release build warning-free (warnings-as-errors on Release, per standards).
- Qodo findings at **zero** before any merge.

**Cross-platform parity check (the one that actually matters):**
Run the same inputs through both apps side by side and diff the output — for each of the 6 methods, a representative dose/ratio, then the full guided timeline (step order, durations, total). Any divergence is a port bug. The 49 shared test cases should catch this mechanically, but do the manual pass once on real devices before M12.

**Manual, on the physical device (cannot be emulated):**
1. Buy Pro with a license tester → Pro methods unlock; confirm acknowledgement lands (check Play Console **Orders**; an unacknowledged purchase auto-refunds).
2. Uninstall → reinstall → **Restore** → entitlement returns.
3. Decline instrument → entitlement does *not* grant.
4. Start a V60 brew → lock screen 3 min → unlock → verify the step and elapsed time are correct, not frozen.
5. Schedule a cold brew → verify the notification arrives (accept modest Doze drift).
6. Deny notification permission → app remains fully usable.

---

## Recommendation

**Do it.** The port is unusually favorable — the ports-and-adapters discipline on iOS means ~1,073 lines transliterate mechanically with 49 tests as the conformance spec, and only ~320 lines of real platform glue need new implementations. Cash cost is ~$105–225 with **no annual fee ever**, against Apple's $99/year.

Sequence it as: **register the Play org account today** using the D-U-N-S already issued to JR Labs, buy a cheap Android phone before M8, and build M1→M2 while account verification runs. The organization account is what makes this attractive — it removes the 12-tester/14-day gate that would otherwise add a month and require sourcing 12 real testers — and because the D-U-N-S is already in hand, that advantage costs no calendar time at all.

The two things most likely to bite: **background timer continuity** (M9 — plan real time for it, not an afternoon) and **purchase acknowledgement** (M8 — get it wrong and real customers get silently refunded).

One scope note: I'd finish this *after* iOS 1.1 clears review, so you're not debugging two submissions at once.
