# CoffeeGrams for Android — v1.0 Google Play Submission Runbook (AS-BUILT)

Reflects the **actual** Play Console flow used to take v1.0 (versionCode 2)
live. Sibling of the iOS as-built doc (`CoffeeGrams/Releases/submission_1.0.md`).
The step-by-step *plan* lives in `play-console-release.md`; this file records
what **actually happened**, in order, including the mistake.

> **Status: LIVE.** Production release **2 (1.0)** — "Available on Google
> Play", full rollout, 178 of 178 countries — went public on
> **2026-10-08** (console showed the Production row updated 10:02 PM, console
> time). Listing confirmed loading and the app installed from it on a physical
> device (user-verified 2026-10-08).

Package: `com.jrlabapps.coffeegrams` · IAP: `com.jrlabapps.coffeegrams.pro`
($4.99, one-time) · Category: **Food & Drink** · Listing name:
**CoffeeGrams: Brew Calculator** · Live URL:
<https://play.google.com/store/apps/details?id=com.jrlabapps.coffeegrams>

> ⚠️ **The headline lesson:** Play's "Published" does not mean *Production*.
> See [What went wrong](#what-went-wrong-published-was-not-production) — the
> first "Publish changes" click only released a **Closed testing** track and
> the app stayed invisible to the public for about a day.

---

## Timeline (as built)

| When | What |
|---|---|
| 2026-08-11 | Upload keystore created (`~/keystores/coffeegrams-upload-key.jks`, alias `coffeegrams`), backed up in the password keeper |
| 2026-09-01 | Signed AAB, **versionCode 2**, uploaded to **Internal testing** (versionCode 1 was already burned by an earlier partial attempt); Play App Signing enrolled automatically on that first upload |
| 2026-09-15 / 09-16 | Physical-device validation on a Galaxy A15: cross-platform parity (all 6 methods) and cold-brew notification delivery — both passed |
| 2026-09-23 | App content finished ("You're all caught up"); Managed publishing turned **on**; foreground-service declaration + `scrcpy`-recorded demo video submitted. **Believed** to be sent for Production review — this belief was wrong (see below) |
| 2026-10-07 ~9:06 PM | User clicked **Publish changes** on "Ready to publish". What actually published: release **2 (1.0)** on **Closed testing – Alpha**. Production was still an empty Draft |
| 2026-10-08 | Public Play URL (incognito) → "not found". Console inspected read-only; diagnosis below |
| 2026-10-08 | Production draft completed (bundle from library, release notes) → **Submit 1 change for review** |
| 2026-10-08 | Google approved (same day). Change appeared under **Changes ready to publish** as Production 2 (1.0) "Start full rollout" |
| 2026-10-08 ~10:02 PM | **Publish 1 change** → confirmed ("usually appears within 1 hour… can't be undone"). Production row: **Available on Google Play** |
| 2026-10-08 | Public listing loads; app installed from the live listing |

The exact clock time Google's approval landed was not captured; the submit
and approval both fell on 2026-10-08.

## What went wrong: "Published" was not Production

**Symptom.** Play Console said "Ready to publish", then "Published". More than
a day later the app could not be found in the Play Store, and the public URL
returned "not found" in an incognito window. The user's phone only showed a
"test app" (the tester build).

**Cause, as found in the console.**

- Test and release → **Latest releases and bundles** had three rows: Internal
  testing (v2), **Closed testing – Alpha (v2, "Available to testers on Google
  Play")**, and **Production = Draft, no version**.
- App list status read **Closed testing**; the app dashboard said Production
  **Inactive**.
- So the "Publish changes" click released the *Closed testing – Alpha* track.
  Nothing had ever been submitted for Production, despite an earlier note
  saying it had been on 2026-09-23. How Alpha came to hold the release on
  2026-10-07 was not established.
- Production's **Create new release** button was greyed out, with the
  tooltip "To create a new release, roll out or discard your current draft
  release" — an **empty draft** already existed.

**Fix.** Releases tab → **Edit release** on the draft → **Add from library**
(versionCode 2, no rebuild or re-upload) → paste release notes inside
`<en-US>…</en-US>` → Next → review (0 errors; 1 non-blocking warning) → Save →
Publishing overview → **Submit 1 change for review** → confirm.

**Review step warning (non-blocking):** "This App Bundle contains native code,
and you've not uploaded debug symbols." Only affects how readable crash/ANR
reports are. Not addressed for 1.0.

**Checklist for any future app** (also saved as a project memory and in the
M13 retrospective inputs):

1. After publishing, open the public URL **logged out / incognito** — that is
   the only test that matches what a user sees.
2. In **Latest releases and bundles**, the **Production** row must show a
   version and "Available on Google Play". "Draft" with "–" means nothing is
   live.
3. In **Publishing overview**, know which section you are in: *Changes not
   yet submitted for review* → *Changes in review* → *Changes ready to
   publish* → published. Confirm the item listed is **Production**, not a test
   track, before clicking the final button.
4. Do not record "submitted to Production" from a note — verify the console.

## Release configuration used

- **Track:** Production, **full rollout** (a brand-new app's first release has
  no staged-rollout percentage; it goes to 100% of users in the chosen
  countries at once).
- **Countries / regions:** 178 of 178.
- **Managed publishing: ON.** Approval did not auto-publish; the go-live was
  a deliberate second click on **Publish changes** / **Publish 1 change**.
- **Release name:** `2 (1.0)` (Play's auto-suggestion, not shown to users).
- **Release notes (en-US):** "The first release of CoffeeGrams for Android.
  Brew better coffee with precise calculators and guided timers for six
  methods. Thanks for trying it — we'd love your feedback." (source:
  `store-listing.md`, "What's new").
- **Bundle:** app bundle 2 (1.0), "Enhanced" badge, API 26+, target SDK 36.
- **Store listing, Data safety, content rating, trader/DSA, target audience
  (18+ only), foreground-service declaration:** see
  `play-console-compliance.md` and `store-listing.md`. All were complete
  ("You're all caught up") before the Production submission.

## Review turnaround

Submitted and approved on the same calendar day (2026-10-08). Google's own
dialog quotes "typically within 7 days, but may take longer" — a first
submission with a completed App content page cleared far faster than that.

## After going live — still to do

- [ ] M12 board card → **Done** once the PR carrying this document merges
- [ ] M13 retrospective (`CoffeeGramsAndroid_Summary.md` in the private
  `Summary` repo + copy of `ARCHITECTURE.md`), including the Play
  "Published ≠ Production" gotcha above and the AI-agents process review
- [ ] Consider uploading native debug symbols for 1.1 to clear the warning
- [ ] Watch crash/ANR rates and reviews in Play Console after launch
