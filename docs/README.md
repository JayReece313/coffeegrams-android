# docs/ — CoffeeGrams for Android public site

These pages are published via **GitHub Pages** (source: `main` branch, `/docs`
folder, Cayman theme) and provide the Play Store listing's two required URLs.

Deliberately a **separate copy** from the iOS `coffeegrams` repo's own
`docs/` — the iOS pages mention SwiftData, the App Store, and Apple Account by
name, so pointing the Android listing at them would be inaccurate. Same
palette-and-brand content, adapted per platform (Room instead of SwiftData,
Google Play Billing instead of the App Store, Google Account instead of Apple
Account). Keep the two in sync by hand when the shared facts (price, free
tier, contact email) change — there's no build-time link between them.

**Live URLs** (once Pages is enabled on this repo — see task tracking in the
M11 milestone)
- Home: https://jayreece313.github.io/coffeegrams-android/
- Privacy Policy: https://jayreece313.github.io/coffeegrams-android/privacy/
- Support: https://jayreece313.github.io/coffeegrams-android/support/

**Files**
- `_config.yml` — Jekyll config (theme, `baseurl: /coffeegrams-android`, excludes this README).
- `index.md` — landing page (`permalink: /`).
- `privacy-policy.md` — `permalink: /privacy/`.
- `support.md` — `permalink: /support/`.

**Editing:** change the markdown, commit, push to `main`; Pages rebuilds in
~1–2 minutes.
