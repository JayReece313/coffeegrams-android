# Play Console compliance forms — answers runbook (DRAFT)

The *answers* below are stable facts about the app and are safe to rely on.
The exact **menu paths** are not — Play Console's navigation has moved
enough times during this project (see the `play-console-navigation-caution`
memory from M8) that specific click-paths are deliberately omitted here.
When we sit down to actually enter these, we'll do it live against
whatever's on screen, the same way M8's billing setup went.

---

## 1. Target audience and content

**App content → Target audience and content** (Play Console gates the Data
safety form behind this one completing first — encountered live
2026-08-22, not something the original draft of this runbook anticipated).

- **Target age group(s):** select **only "Ages 18 and over."** Nothing else.
  Selecting any bracket under 18 (and especially under 13) pulls the app
  into Google Play Families policy requirements — extra content and data
  rules that don't fit a coffee/caffeine product with no child-directed
  design. Sticking to 18+ only also skips every Families follow-up
  question the form would otherwise ask.
- **"Does your app's design or content appeal to children even if it's not
  the target audience?"** (or equivalent wording) → **No.**
- **Ads** (if asked here rather than under Data safety) → **No** — the app
  has no ad SDK.

Verified against Google's own current help page for this section
(`support.google.com/googleplay/android-developer/answer/9867159`) rather
than guessed, per the navigation-caution lesson from M8.

## 2. Data safety form

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

## 3. IARC content rating questionnaire

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

## 4. EU trader (DSA) status

**Correction (2026-08-23):** there is no separate "declare trader status"
screen in current Play Console — confirmed both by hitting a dead end
looking for one under App content, and by the user's own research into
Google's current stated behavior. **Google infers trader status
automatically** from two existing account-level settings, both under the
left-nav **Settings** section (account-level, not per-app "App content" —
that's why it wasn't findable there):

1. **Settings → Developer account → Details.** If **Account Type =
   Organization**, Google treats the account as a trader by default and
   publishes the business address to EU users. This is already true here —
   the account was registered as an **Organization** under **JR Labs LLC**
   at M0 (see `README.md`'s M0 line) — so this half is already satisfied,
   just worth opening this page to confirm the legal name/address on file
   is current and correct before EU users start seeing it published.
2. **Settings → Payment profile.** An active merchant profile processing
   paid apps/IAPs triggers the trader flag, and publishes that profile's
   legal name, address, phone, and email to EU users. A payments profile
   should already exist from M8's Play Billing setup (the "CoffeeGrams
   Pro" IAP requires one to process payments) — confirm it's active and
   the published contact details are correct, same "worth a look before it
   goes live" reasoning as #1.

So the task here isn't "declare Trader" (there's nothing to click) — it's
**verify both pages show accurate, already-public-safe business info**,
since whatever's on them ships to EU users automatically once the app is
live. If neither page reflects a commercial/organization setup for some
reason, that's the actual problem to fix, not a missing declaration.

- **Business name:** JR Labs LLC
- **Email:** info@jrlabapps.com
- Address/phone: whatever is currently on the Developer account Details
  and Payment profile pages — pull the actual values from there rather
  than retyping from memory, since a wrong public business address is
  the kind of mistake that's annoying to walk back after publish.

## 5. Store listing text & assets

Covered separately in `Releases/store-listing.md` (title, descriptions,
category) and `Releases/store-assets/` (512×512 icon, 1024×500 feature
graphic). Phone screenshots are `Releases/screenshots/01`–`05` from M10,
already at Play's recommended 1080×1920.

---

*Draft — none of these have been entered into Play Console yet. This is
prep for that live session, not a record that it happened (that's what
`Releases/submission_1.0.md` will be, written as-built once it's done, per
M12).*
