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

**`BrewSessionNotifier` (M9), landed:** `GuidedBrewViewModelTest` gained 4
cases against the new `RecordingBrewSessionNotifier` double — starting a
brew calls the notifier's `start()`, `finish()` calls `stop()`, ticking
while active calls `update()`, and (the one guarding against a real gap)
clearing the `ViewModel` via a real `androidx.lifecycle.ViewModelStore`
without ever calling `finish()` still calls `stop()` — covers the user
backing out of the screen mid-brew, not just the happy-path end. What's
*not* unit-tested: the actual `BillingClient`-style live surface here is
`BrewTimerForegroundService`/`startForeground()` itself, which needs a real
process and a real Android framework — same "cannot be automated without a
device" category as `LivePurchases`, verified instead by
`connectedAndroidTest` starting the real service without crashing, plus the
physical-device Doze/backgrounding checklist below.

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

**Screenshot harness (M10):** `ScreenshotCaptureTest.kt` (top-level package,
not scoped to one screen — it drives the whole app) mirrors the iOS
sibling's `ScreenshotCaptureTests.swift`/`capture.sh` pattern: the same
five assertion-only tests run in every normal `connectedAndroidTest` pass
(so a button-text rename fails CI, the guard against the Play listing
quietly going stale), while the actual shutter is opt-in behind a
`captureScreenshots` instrumentation argument only
`Releases/screenshots/capture.sh` sets — a plain suite run pays none of
the screenshot cost. Unlike every other Compose UI test here, it uses
`createAndroidComposeRule<MainActivity>()` (not `createComposeRule()` +
an isolated `setContent {}`), driving the real app — real
`CoffeeGramsApplication`, real Room, real navigation — the same way
`XCUIApplication().launch()` does on iOS, since a screenshot of a test
harness isn't a screenshot of what ships. Needs `GrantPermissionRule` for
`POST_NOTIFICATIONS` for the same reason `GuidedBrewScreenTest` does
(M9): starting a guided brew without it launches the real system
permission dialog, which sits outside Compose's test tree and fails
every assertion past that point with "no compose hierarchies found."

Run `./Releases/screenshots/capture.sh` (optionally with one of
`01-home`/`02-calculator`/`03-guided-timer`/`04-paywall`/`05-brew-log` to
capture just one) to produce the actual listing images: it builds and
installs both APKs, clears app data first (so the brew-log shot shows a
clean single entry rather than accumulating across runs), pins the status
bar to 9:41/full battery via Android's "Demo Mode" broadcasts (the direct
equivalent of iOS's `simctl status_bar override`), runs the capture test
with the shutter enabled, pulls the frames off the device, and fits each
to 1080×1920 (Play's own recommended phone screenshot size — unlike
Apple's exact-pixel-match requirement, Play actually accepts anything
from 320–3840px per side at a 16:9–9:16 aspect ratio, so this fit-down is
for consistency across the set, not because Play demands it). Needs a
connected device/emulator, same assumption `connectedAndroidTest` already
makes — this script doesn't boot one itself.

One thing the `TOTAL` guided-timer assertion caught during development:
`Thread.sleep()` on the test thread does **not** let the app's real tick
loop advance, since it doesn't synchronize with Compose's own idling
mechanism — a raw sleep left `TOTAL 0:00` frozen even after 8 real
seconds. `composeTestRule.waitUntil { ... }` (which polls with genuine
synchronization each iteration) is what actually lets real ticks land;
worth remembering for any future capture-mode wait.

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
Console app.

**One-time upload required before Play Console allows creating the in-app
product** (discovered during M8, 2026-08-11): Play Console's Monetize →
Products → One-time products screen refuses to let you create a product
at all until it has seen the `com.android.vending.BILLING` permission in at
least one uploaded build — "Your app doesn't have any one-time products yet.
To add one-time products, you need to add the BILLING permission to your
APK." A signed release build uploaded once to the **Internal testing**
track (fastest, no review) satisfies this — generate an upload keystore via
Android Studio's Build → Generate Signed Bundle/APK, enroll in Play App
Signing on first upload, then create the release.

**Purchase testing needs the app installed *through* the Play Store from
that testing track — a sideloaded `adb install`/`./gradlew
:app:installDebug` build is not enough**, even though it has the same
`BILLING` permission and package name. Play's sandbox (free purchases for
license testers) only activates for a testing-track install. Each tester's
Google account also has to individually open the track's opt-in link and
accept becoming a tester — being added to the Testers list doesn't
pre-accept it for them. Practical flow per tester account: sign into the
Play Store app as that account on the device → open the Internal testing
opt-in link in a browser → accept → tap through to install from Play Store
(not adb).

**License testing is at Play Console → Settings → License testing** — not
under "Setup" or "Advanced settings" (both were dead ends chased during
M8). This is a *different* list from the Internal testing track's own
Testers list (Testing → Internal testing → Testers) — a tester account
needs to be on **both**: the Testers list to install the app at all, the
License testing list for purchases to be free rather than real charges.

**Expect propagation delays at multiple points, and don't mistake them for
misconfiguration.** "Item not found" / "could not be found" showed up
during M8 for at least three different not-yet-propagated states: a
newly-activated in-app product, a newly-added license tester (purchases
went through as *real charges*, not free, until enough time passed), and a
newly-accepted testing-track opt-in (couldn't download until later). None
of these had a fixed, documented wait time in practice — waiting and
retrying resolved all three. If a real charge happens because of this
(it did, once, during M8), refund it via Play Console → **Order
management** (search by order ID or tester email) — note that a refund
does not necessarily revoke the entitlement, so a refunded purchase can
still show as owned on the device afterward (which incidentally is a
perfectly good way to exercise the restore-purchase code path without
buying again — `LivePurchases.restore()` is a literal one-line delegation
to `isPurchased()`, so proving entitlement persists after reinstall proves
`restore()` too, whether it got there via the button or the automatic
`PurchaseController.start()` check on launch).

The "Countries/regions" tab under a release track only exists for
**Production**, not testing tracks — don't go looking for it on Internal
testing; there's nothing there to configure for testing purposes.

| # | Check | Passes when | Verified |
|---|---|---|---|
| 1 | Buy Pro as a license tester | Pro methods unlock **and** the purchase is acknowledged — verify in Play Console → Orders | ✅ live device, 2026-08-12 |
| 2 | Uninstall → reinstall → Restore | Entitlement returns via `queryPurchasesAsync()` | ✅ live device, 2026-08-12 (see note below) |
| 3 | Decline the payment instrument | Entitlement is **not** granted | Unit test + code review only (see note below) |
| 4 | Already-owned purchase attempt | Handled gracefully, no error state | Unit test + code review only (see note below) |

> ⚠️ **Acknowledgement is mandatory on Play, unlike StoreKit's `finish()`.** An
> unacknowledged purchase is **auto-refunded** — 3 days in production, minutes for
> test purchases. Getting this wrong silently refunds paying customers. Never ship
> a billing change without re-running check 1.

**Why checks 3 and 4 stopped at unit-test + review coverage rather than a live
repro (M8, 2026-08-12):** two accidental *real* charges happened during M8's
device testing (both from the license-tester propagation-delay gotcha above,
refunded via Order management), and both refunds left the purchase still
owned — Order management's **"Remove entitlement"** checkbox only appears on
the *original* refund action, not retroactively once a refund has already
processed (confirmed by trying it; there's a whole Google Play Developer
Community thread of people hitting this same "forgot to check it" trap).
With both tester accounts durably showing Pro as owned and no way to reset
either, live-repro'ing check 3 (decline) or 4 (already-owned) would have
needed a third Google account through the entire opt-in + propagation cycle
again, for two of the lower-risk paths. Both are covered instead by
`LivePurchasesTest`'s `classifyPurchaseResponse` cases
(`USER_CANCELED`/`ITEM_ALREADY_OWNED`) and `PurchaseControllerTest`'s
"a cancelled purchase leaves it locked" (M6) — decline/cancel and
already-owned are also the most heavily-exercised, best-understood outcomes
in the Play Billing library generally, unlike acknowledgement (check 1) and
entitlement persistence (check 2), which were the two paths that actually
carried real risk and did get proven on hardware.

### Timer continuity and notifications (M9)

`platform/BrewTimerForegroundService.kt` + `platform/LiveBrewSessionNotifier.kt`
(M9) exist so checks 5–9 pass at all — a backgrounded process with no
foreground service is a plausible OOM-kill target, which would take the
`GuidedBrewViewModel` tick loop (and the brew) with it. Scoped to guided
brew only (V60, Chemex, French Press, AeroPress) — not espresso shots,
which run 20-40 seconds and don't carry the same risk.

To force Doze on-demand for check 7, rather than waiting for the real
thing: `adb shell dumpsys battery unplug` then
`adb shell dumpsys deviceidle force-idle` (exit with
`adb shell dumpsys deviceidle unforce`).

**Forward dependency for M11/M12, not needed for M9 itself:** Play Console
requires a matching foreground-service-type declaration on the app's "App
content" page before submission, alongside the manifest's
`PROPERTY_SPECIAL_USE_FGS_SUBTYPE` justification string that Play reviews.

**Real gap found and fixed during the device pass (2026-08-14): guided
brew never requested `POST_NOTIFICATIONS`.** The existing runtime-permission
request flow (M7) only triggers from the cold-brew "start steep" button. A
guided brew started without ever touching cold brew first had no permission
path at all — `LiveBrewSessionNotifier` still called `NotificationManagerCompat.notify()`
correctly, but Samsung's OS silently suppressed display of it
("`Suppressing notification from package ... by user request`" in logcat,
confirmed via `adb shell dumpsys notification` showing `importance=NONE`
at the app level even though the channel itself was correctly configured
`IMPORTANCE_LOW`). Also observed a `FGS stop call ... has no types`
warning seconds after the service started with the permission missing,
suggesting Samsung may stop a foreground service early when it has no
visible notification to justify staying alive — exactly the protection M9
exists to provide. Fixed by giving `GuidedBrewViewModel` its own
`NotificationScheduling` dependency (reusing the same port cold brew
already uses) and requesting the permission from `GuidedBrewScreen`'s
Start button, mirroring `ColdBrewScreen`'s existing pattern exactly — see
`GuidedBrewViewModel`'s doc comment for why `start()` doesn't wait on the
prompt's result the way cold brew's flow does.

| # | Check | Passes when | Verified |
|---|---|---|---|
| 5 | Start a V60 brew, lock the screen 3 min, unlock | Step and elapsed time are **correct**, not frozen and not reset | ✅ 2026-08-14 |
| 6 | Start a brew, swipe the app away | Behaviour is defined and the foreground-service notification is accurate | ✅ 2026-08-14 (after the `POST_NOTIFICATIONS` fix above — see that note for what failed first) |
| 7 | Leave a brew running into Doze | Step transitions still land | ✅ 2026-08-14, via `dumpsys deviceidle force-idle` |
| 8 | Rotate the device mid-brew | Timer survives configuration change | ✅ 2026-08-14 |
| 9 | Take an incoming call mid-brew | Timer survives | Covered by generalization from checks 5–8, not directly tested — the test device has no cellular radio. A call interrupts the app the same way backgrounding does; nothing about a call gets different process-lifecycle treatment on Android. |
| 10 | Schedule a cold brew, wait 12–24 h | Notification arrives; **modest Doze drift is acceptable** and is not a bug | Pending — pre-existing M5 functionality, not new M9 work |
| 11 | Deny the notification permission | App remains fully usable | ✅ observed directly, 2026-08-14 — checks 5 and the first attempt at check 6 both ran correctly (brew progressed, no crash) while `POST_NOTIFICATIONS` was still denied, before the fix above; only the notification's visibility was affected |

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
