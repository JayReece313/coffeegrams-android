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

**Status: ✅ LIVE 2026-10-08.** App content was complete on 2026-09-23,
but the Production release was **not** actually submitted then — an empty
Production draft was left behind, and the first "Publish changes" click
(2026-10-07) only released the Closed testing – Alpha track. The Production
release was completed and sent for review on 2026-10-08, approved the same
day, and published with **Publish 1 change**. Verify any "submitted"
claim in the console (Latest releases and bundles → Production row), not
from notes. No Play Developer API access is set up in this project, so
review status has to be checked manually in Play Console. Full as-built
sequence and the diagnosis checklist: `Releases/submission_1.0.md`.

## 6. `Releases/submission_1.0.md`

✅ Written as-built — see `Releases/submission_1.0.md`.

---

*Steps 1-6 are done. v1.0 (versionCode 2) is live on Google Play as of
2026-10-08. Remaining: M12 board card to Done after the PR carrying the
as-built doc merges, then M13 (retrospective).*
