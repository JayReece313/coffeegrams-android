# Play Console compliance forms — answers runbook (DRAFT)

The *answers* below are stable facts about the app and are safe to rely on.
The exact **menu paths** are not — Play Console's navigation has moved
enough times during this project (see the `play-console-navigation-caution`
memory from M8) that specific click-paths are deliberately omitted here.
When we sit down to actually enter these, we'll do it live against
whatever's on screen, the same way M8's billing setup went.

---

## 1. Data safety form

**App content → Data safety → Start** (per Google's current help page,
verified 2026-08-14 — confirm this is still where it lives when we get
there).

CoffeeGrams collects **zero** user data — no accounts, no analytics, no
ads. `app/build.gradle.kts` has exactly one non-AndroidX/Kotlin runtime
dependency, `billing-ktx` (Google Play Billing), used solely to process the
one-time Pro purchase — it doesn't route personal data through us, Google
handles that entirely. That's the one SDK in the app; it's not an
analytics/tracking/ad SDK, which is what the Data safety form's questions
are actually probing for.

- **"Does your app collect or share any of the required user data types?"**
  → **No**.
- Follow-up encryption/deletion questions → answer per Google's prompts
  (moot once "No" is selected for data collection, but the form still asks
  them — answer honestly: data in transit is HTTPS-only Play Billing
  traffic we don't touch, nothing is collected so there's nothing to
  request deletion of).
- **Privacy Policy URL** (required even with no data collected):
  `https://jayreece313.github.io/coffeegrams-android/privacy/`
- **Submit/Publish** the declaration — matches iOS's "must click Publish or
  it stays flagged" lesson; confirm the equivalent action here completes
  fully rather than leaving it in a draft state.

Expected result on the listing: **"No data shared with third parties" /
"No data collected."**

## 2. IARC content rating questionnaire

**App content → Content rating** (verify current path on screen).

Same shape as the iOS Age Rating answers — the app has no violence, no
user-generated content, no gambling, no in-app chat, no location sharing,
no web browser, nothing that would raise a rating above the floor:

- Violence, sexual content, profanity, controlled substances, gambling →
  **None/No** across the board.
- User-generated content / user interaction (chat, sharing) → **No** (no
  accounts, no social features).
- Shares location → **No**.
- Digital purchases → **Yes** (the one-time "CoffeeGrams Pro" IAP) — this
  is the one **Yes** in the whole questionnaire; expect it to still land at
  the lowest tier (Android's rough equivalent of iOS's 4+) since it's the
  only flag raised.

## 3. EU trader (DSA) declaration

**App content → EU business/trader details** (verify current path — this
section has been renamed and relocated even within the App content page
across recent Play Console revisions).

Declare as a **Trader**, not "not a trader" — same reasoning as the iOS
submission (`coffeegrams/Releases/submission_1.0.md` §6): CoffeeGrams Pro
is a paid product sold commercially through JR Labs LLC. Declaring
"not a trader" removes the app from EU storefronts entirely, which we
don't want.

- **Business name:** JR Labs LLC
- **Contact details** (public, per DSA requirement): reuse whatever
  registered-agent/business address and phone were used for the iOS trader
  declaration — pull the actual values from wherever that was recorded for
  the iOS submission rather than re-typing from memory here, since a wrong
  public business address is the kind of mistake that's annoying to walk
  back after publish.
- **Email:** info@jrlabapps.com

## 4. Store listing text & assets

Covered separately in `Releases/store-listing.md` (title, descriptions,
category) and `Releases/store-assets/` (512×512 icon, 1024×500 feature
graphic). Phone screenshots are `Releases/screenshots/01`–`05` from M10,
already at Play's recommended 1080×1920.

---

*Draft — none of these have been entered into Play Console yet. This is
prep for that live session, not a record that it happened (that's what
`Releases/submission_1.0.md` will be, written as-built once it's done, per
M12).*
