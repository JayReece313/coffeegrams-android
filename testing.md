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

**Emulator AVDs — avoid `system-images;android-37.0;google_apis;arm64-v8a` on
the primary dev Mac (M4 Max, macOS 26.3.1):** it crashes on boot with a
`SIGSEGV` inside QEMU's `AddressSpaceDevicePing` (the graphics-passthrough
channel) — a genuine QEMU bug, not a config mistake, and unrelated to `-gpu`
flags. Use `system-images;android-34;google_apis;arm64-v8a` instead (revision
14 as of 2026-08-08), which boots clean. Reconfirm this if the emulator
package or either system image is ever updated.

**A coding-agent session cannot itself run the emulator on this machine** —
launching `qemu-system-aarch64` from inside a sandboxed coding session hits
`mprotect: Permission denied` regardless of flags, even though `sysctl
kern.hv_support` and the binary's own codesigning entitlements are both fine.
The emulator must be launched from a normal, user-driven terminal outside the
session; `adb`/`gradlew :app:installDebug`/screenshotting from inside the
session works fine once a device is already up and registered with `adb`.

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

JVM tests for the five ViewModels (M6) and the platform adapters (M5),
against the in-memory/recording/scripted port doubles for storage, clock,
haptics, notifications, and purchases. **Turbine** is a test dependency for
asserting `StateFlow`/`Flow` emission *sequences*; the M6 suite doesn't need
it — every ported iOS test case asserts a single synchronous snapshot after
an action, matching the iOS suite's own `#expect` pattern, so a plain
`.value` read is the direct equivalent. Reach for Turbine when a future test
actually needs to assert an emission sequence over time.

**Persistence (M4), landed:** `BrewLogEntityMappingTest` (4 cases) proves the
`BrewLogEntity` ↔ `BrewLogEntry` mapping round-trips and that an unrecognized
`methodRawValue` falls back to V60, matching iOS's `BrewLogRecord.method`
exactly. `InMemoryBrewLogStoreTest` (8 cases) exercises the full
`BrewLogStoring` contract — add/entries round-trip, newest-first ordering,
delete, setRating/setNotes, and no-op-on-missing-id — against the in-memory
double. `RoomBrewLogStoreTest` (`androidTest`, below) asserts the identical
8 cases against real Room.

**Platform adapters (M5), landed:** the three test doubles
(`FakeAdvancingClock`, `RecordingHaptics`, `RecordingNotificationScheduler`)
each have their own unit test, mirroring `InMemoryBrewLogStore`'s precedent
of testing the double itself. Two pure functions extracted from the
framework-touching live code are also unit-tested on the plain JVM:
`LiveNotificationScheduler.buildWorkRequest` (reminder → `OneTimeWorkRequest`
mapping, delay clamping, tagging) and `ReminderWorker.contentFrom` (input
`Data` → notification content, including the `String` id → `Int` notification
id derivation). What stays unautomated: the live adapters' actual framework
calls (`LiveMonotonicClock`, `LiveHaptics`, the channel/`WorkManager` side of
`LiveNotificationScheduler`, `ReminderWorker.doWork`'s `notify()` call) — no
`work-testing` or Robolectric dependency exists in the catalog, and this
mirrors the iOS sibling's own precedent of never unit-testing
`LiveNotificationService`/`LiveHaptics`/`SystemClock` directly. Actual
notification *delivery* is verified manually on a device/emulator, the same
way Play Billing and Doze are scoped below.

**`LivePurchases` (M8), landed:** the same live-adapter-vs-pure-function
split applies. `classifyPurchaseResponse` — the `BillingResponseCode`/
`Purchase.PurchaseState` → `PurchaseOutcome` mapping — is pulled out as a
function of plain `Int`s and covered by 6 cases in `LivePurchasesTest`. The
rest of `LivePurchases` (the actual `BillingClient` connection, purchase
flow, and acknowledgement calls) is exactly the "cannot be automated" case
described below: it needs a real Play Store connection that neither a unit
test nor the emulator can provide.

**ViewModels (M6), landed:** 39 cases across 7 files, conformance-matched to
the iOS sibling's ViewModel test suites (same inputs, same expected values).
`CalculatorViewModelTest` (9) + `BrewPresetTest` (1) need no test double —
pure value logic. `GuidedBrewViewModelTest` (14) and `EspressoShotViewModelTest`
(4) drive `FakeAdvancingClock`/`RecordingHaptics` and call `tick()` directly,
exactly like iOS calls `tickOnce()` — the real `viewModelScope`-owned ticker
(started/stopped in lockstep with the timer's active state, not running
unconditionally) never actually fires during a test because no test calls
`advanceUntilIdle()`/`advanceTimeBy()`, so it stays parked at its `delay()`
for the test's duration regardless of whether it's been started. Constructing
either VM requires `Dispatchers.setMain(StandardTestDispatcher())` in
`@BeforeTest`/`Dispatchers.resetMain()` in `@AfterTest`, since
`viewModelScope.launch` resolves `Dispatchers.Main` immediately when first
called. `ColdBrewViewModelTest` (3) reuses M5's
`RecordingNotificationScheduler` (extended with an `authRequestCount`
counter to match the iOS spy's `authRequests`). `BrewReminderTest` (2)
covers the pure reminder-content builder. `PurchaseControllerTest` (6) uses
a new `ScriptedPurchases` double against the `Purchases` port pulled forward
from M8 (same early-port/late-adapter split as `MonotonicClock`).

**`LogViewModel`/`LogDetailViewModel` (M7 PR3), landed:** 8 cases across 2
files, against `InMemoryBrewLogStore` (the same test double M4 built).
Unlike M6's ViewModels above, these use
`Dispatchers.setMain(UnconfinedTestDispatcher())` rather than
`StandardTestDispatcher()`: every `viewModelScope.launch` here runs against
an in-memory map with no real suspension point, so eager/synchronous
execution is what lets assertions read post-launch state without a separate
`advanceUntilIdle()` call — `StandardTestDispatcher` would leave the launch
queued and the assertion would see stale state. `LogViewModelTest` (3) —
loads newest-first on init, starts empty, delete refreshes the list.
`LogDetailViewModelTest` (5) — loads the matching entry by id, `setRating`
updates local state immediately and persists, tapping the current rating
clears it back to unrated, `saveNotes` trims and empty becomes `null`,
`delete` invokes its callback.

### 3. Instrumented tests (`androidTest`)

```bash
./gradlew :app:connectedAndroidTest
```

Needs a running emulator or a connected device — see the emulator note above
if setting one up locally.

**Persistence (M4), landed:** `RoomBrewLogStoreTest` (8 cases) — the exact
same `BrewLogStoring` contract as `InMemoryBrewLogStoreTest`, run against a
real (in-memory-mode) Room database, proving the actual SQL, `TypeConverters`
(`UUID`↔`TEXT`, `Instant`↔epoch-millis `INTEGER`), and DAO wiring are correct,
not just the interface contract in isolation.

**Compose UI tests, all 5 screens landed across M7 PR1–PR3:** 30 cases
across 8 files, using `androidx.compose.ui.test.junit4.v2.createComposeRule`
(the current non-deprecated API).

- `MethodPickerScreenTest` (4) — an unlocked method navigates, a locked one
  opens the paywall instead of navigating, the toolbar "Unlock Pro" action
  also opens it, the toolbar "Brew log" action navigates to the log *(PR3)*.
- `CalculatorScreenTest` (7) — mode toggle switches the input label, the
  ratio slider carries a content description ("Brew ratio"), the CTA
  renders with its method-specific label and navigates on tap, AeroPress
  presets render.
- `PaywallScreenTest` (3) — all 4 benefits render, the buy button never
  fabricates a price when `priceText` is null, restore doesn't dismiss the
  sheet when there's nothing to restore. Since M8, both this and
  `MethodPickerScreenTest` inject `PurchaseController(UnavailablePurchases())`
  explicitly via each screen's `purchases` override param, rather than
  relying on `CoffeeGramsApplication`'s own default — which is the real
  `LivePurchases`/`BillingClient` adapter from M8 onward, and would
  otherwise mean these tests silently attempt a live billing connection.
- `GuidedBrewScreenTest` (4), `EspressoShotScreenTest` (4),
  `ColdBrewScreenTest` (2) *(PR2)* — timer/step rendering and the "Save to
  Log" button's presence; the actual Room write isn't exercised from these
  tests (see below).
- `LogScreenTest` (3), `LogDetailScreenTest` (3) *(PR3)* — the empty state,
  tapping a row navigates with its id, deleting a row removes it from the
  store; the detail screen's summary rendering, tapping a star persists the
  rating, and deleting invokes `onDeleted`.

**Why the log screens build their own in-memory Room database per test**
(mirroring `RoomBrewLogStoreTest`'s `@Before`/`@After`) rather than reading
through `currentApplication().brewLogStore`: that would hit the real
on-device database and leak saved rows between test runs, since Room isn't
reset between instrumented test runs the way `test`-sourceSet doubles are.
`LogScreen`/`LogDetailScreen` both take an optional `store: BrewLogStoring`
parameter (defaulting to `currentApplication().brewLogStore`) for exactly
this seam — the same reason `GuidedBrewViewModel` etc. take their ports as
constructor parameters instead of reaching for the application singleton
internally. This is also why PR2's guided-brew/espresso/cold-brew screen
tests stop at asserting the "Save to Log" button is present rather than
tapping it through to a real write — those screens read
`currentApplication().brewLogStore` directly with no override seam yet;
revisit if a future PR needs to assert the save itself.

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
