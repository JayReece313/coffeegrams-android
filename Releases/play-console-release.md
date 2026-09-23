# Play Console release runbook (DRAFT) — M12

Same discipline as `Releases/play-console-compliance.md`: the *facts* below
are verified against Google's current help pages (2026-08-26), not
recalled from training knowledge — see the `play-console-navigation-caution`
memory for why that distinction matters on this project. Exact menu
wording may still drift; match to whatever's actually on screen.

**Important correction to `PLAN.md`'s framing:** a brand-new app's **first**
Production release does **not** support a staged rollout — Play only
offers staged percentages for *updates* to an app that's already live.
Clicking "Start rollout to production" on v1.0 publishes to 100% of users
in the selected countries immediately, no percentage slider. The actual
safety net for a first release is thorough validation in Internal testing
*before* Production, not a rollout percentage — that's why step 4 below
(physical-device validation) happens before step 5 (Production), not
after.

---

## 1. Generate the upload keystore (you run this)

```sh
keytool -genkeypair -v \
  -keystore coffeegrams-upload.jks \
  -alias coffeegrams \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storetype PKCS12
```
Save at the repo root (already `.gitignore`d — see `*.jks` there). Keytool
prompts for store password, key password, and distinguished-name fields;
use "JR Labs LLC" for the organization field, matching the iOS
distribution cert. **Back it up in at least two places outside this
machine** (password manager attachment + a second location) before doing
anything else — losing it is unrecoverable, and re-keying an already-live
app on Play is a slow last-resort process, not a quick fix.

Then create `keystore.properties` (copy `keystore.properties.example`,
also git-ignored) with the real path and passwords.

## 2. Build and verify the signed AAB

```sh
./gradlew bundleRelease
jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab
```
`jarsigner -verify` should report "jar verified" — confirm this before
uploading anything.

## 3. Upload to Internal testing

**Left nav → Testing → Internal testing** (some Play Console layouts show
this as **Test and release → Testing → Internal testing** — match to
what's actually on screen).

- Create a new release, upload `app-release.aab`.
- **Play App Signing enrollment is automatic for a new app** — verified
  directly, this is not an opt-in click. The first AAB upload
  auto-enrolls the app in Google-managed signing keyed off the upload key
  you just generated; there's nothing to click to "turn it on." An
  advanced option to supply your own app signing key instead of Google's
  generated one exists but only matters before releasing beyond Internal
  testing, and there's no reason to use it here.
- Tester list / feedback URL: reuse whatever was set up for M8's Play
  Billing device testing — a new app's first Internal testing upload
  doesn't need full store-listing info to go out to existing testers.
- Confirm your existing license tester(s) from M8 can install the build.

## 4. Physical-device validation — done

Both checks that were open are now complete, on real devices:

1. **Cross-platform parity** (`testing.md`, "Cross-platform parity check"
   section) — ✅ 2026-09-15. All 6 brew methods (V60, Chemex, French
   Press, AeroPress, Cold Brew, Espresso): calculator output and guided
   timeline (step order, durations, total time) matched exactly between
   the iOS and Android apps. No divergence found.
2. **Cold brew notification delivery** (`testing.md` check #10) — ✅
   2026-09-16, live device.

Everything else in PLAN.md's pre-M12 list — purchase acknowledgement,
restore, lock-screen timer continuity, deny-notification-permission — was
already done, see `testing.md`'s M8/M9 tables.

**Not reopening:** decline-payment-instrument / already-owned-purchase
(`testing.md` checks 3-4) — deliberately left at unit-test + review
coverage after M8's real-money tester-account incident. Don't re-litigate
this; it's a considered decision on record, not a gap.

## 5. Promote to Production

As established above: a first release publishes to **100% of users
immediately** once live, whatever countries/regions are selected — there
is no staged-rollout percentage. To keep the actual go-live moment a
deliberate action rather than something that happens silently whenever
Google finishes reviewing, **Managed publishing** was turned on first
(Play Console → Publishing overview → "Managed publishing status" → Turn
on managed publishing → Save) — this holds an approved release in a
"Changes ready to publish" state until you manually click **Publish
changes**, instead of auto-publishing on approval.

**Extra App content item found along the way, not in the original
runbook:** Play's April 2026 policy update requires a description **and**
a demo video link for the `FOREGROUND_SERVICE_SPECIAL_USE` permission
(`BrewTimerForegroundService`, M9). Recorded via `scrcpy` (mirroring +
recording the physical device from the Mac, over USB — avoided the
overlay/UI problems every on-device screen recorder tried on the Galaxy
A15 introduced), trimmed to the relevant ~95s, uploaded to YouTube
unlisted, and submitted with a description covering why the timer can't
be paused/restarted (the physical brew keeps happening in real time
regardless of what the phone is doing).

**Status: ✅ 2026-09-23** — App content fully complete ("You're all
caught up" per Play Console), release previewed/confirmed, and **sent to
Google for review**. Typical first-submission review is hours to about a
week (per `PLAN.md`). No Play Developer API access is set up in this
project, so review status has to be checked manually in Play Console —
it can't be polled automatically. Once approved, it lands in "Changes
ready to publish"; **Publish changes** is the final, explicit click that
actually goes live, still gated on real-time go-ahead.

## 6. `Releases/submission_1.0.md`

Write this as-built once steps 1-5 actually happen, mirroring the iOS
sibling's own as-built doc — not drafted speculatively ahead of time,
since the exact sequence (timestamps, what got clicked when, any hiccups)
is the point of that document.

---

*Steps 1-5 are done: keystore generated and backed up, signed AAB built
and verified, uploaded to Internal testing (versionCode 2, after
versionCode 1 was consumed by a failed first attempt), both physical-
device validation checks passed, and the Production release has been
sent to Google for review (managed publishing on, so approval won't
auto-publish). Only step 6 (as-built submission doc, written once this
fully lands) and the final **Publish changes** click remain.*
