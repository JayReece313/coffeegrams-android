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

## 4. Physical-device validation (do this before Production, not after)

Exactly two checks are still open per `testing.md` (everything else in
PLAN.md's pre-M12 list — purchase acknowledgement, restore, lock-screen
timer continuity, deny-notification-permission — is already done, see
that file's M8/M9 tables):

1. **Cross-platform parity** (`testing.md`, "Cross-platform parity check"
   section): for each of the 6 brew methods, run a representative
   dose/ratio and the full guided timeline through both the iOS and
   Android apps side by side, diff the results. Any divergence is a
   `:core` port bug.
2. **Cold brew notification delivery** (`testing.md` check #10): schedule
   a cold brew, wait 12-24h, confirm the notification arrives. Modest Doze
   drift is fine.

**Not reopening:** decline-payment-instrument / already-owned-purchase
(`testing.md` checks 3-4) — deliberately left at unit-test + review
coverage after M8's real-money tester-account incident. Don't re-litigate
this; it's a considered decision on record, not a gap.

## 5. Promote to Production

**Only after step 4 is clean, and only with your explicit go-ahead in the
moment** — I won't initiate this myself. Left nav → **Production** →
create a release → add the same AAB (or promote the Internal testing
release directly, Play supports promoting a release from one track to
another without re-uploading) → **Start rollout to production**.

As established above: this publishes to **100% of users immediately** in
whatever countries/regions are selected — there is no staged-rollout
percentage for a first release. Double-check the countries/regions
selection and the store listing (already complete per M11) one more time
before clicking, since there's no gradual-exposure safety net to catch a
mistake here the way there would be on a later update.

## 6. `Releases/submission_1.0.md`

Write this as-built once steps 1-5 actually happen, mirroring the iOS
sibling's own as-built doc — not drafted speculatively ahead of time,
since the exact sequence (timestamps, what got clicked when, any hiccups)
is the point of that document.

---

*Draft — none of these steps have been executed yet. Steps 1 (keystore)
and 3/5 (Play Console uploads/rollout) are yours to run; I'll build the
signed AAB (step 2) once `keystore.properties` exists, and help interpret
whatever Play Console shows at each step.*
