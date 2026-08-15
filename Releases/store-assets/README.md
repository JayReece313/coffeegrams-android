# Play Store listing assets

Two files Play Console asks for that the app's own `res/` resources don't
directly produce (those are code-shipped adaptive-icon XML, not flat PNGs
for a store listing).

## `icon-512x512.png`

Play's 512×512 32-bit PNG store icon. This is the iOS repo's
`coffeegrams_logo/CoffeeGramsIcon.png` (1024×1024, full art on the cream
field) downscaled with `sips`. That source is the exact art
`ic_launcher_foreground.xml` was transliterated from for pixel parity with
the shipping iOS icon (see that file's own doc comment), so reusing it
directly here — rather than re-deriving from the vector drawable — keeps
the Play icon pixel-faithful to the same source.

```sh
gh api repos/JayReece313/coffeegrams/contents/coffeegrams_logo/CoffeeGramsIcon.png \
  --jq '.content' | base64 -d > CoffeeGramsIcon.png
sips -z 512 512 CoffeeGramsIcon.png --out icon-512x512.png
```

## `feature-graphic-1024x500.png`

Play's 1024×500 feature graphic — no iOS equivalent exists, this is new
composition work. `feature_graphic.swift` (a macOS CoreGraphics script, a
design tool, not part of the shipping app — same pattern as the iOS repo's
`coffeegrams_logo/render.swift`) draws the cream gradient field, the brand
mark, the "CoffeeGrams" wordmark, and a tagline, using `DESIGN.md`'s exact
light-mode token hex values.

## Regenerate

```sh
cd Releases/store-assets
gh api repos/JayReece313/coffeegrams/contents/coffeegrams_logo/CoffeeGramsLogoMark.png \
  --jq '.content' | base64 -d > CoffeeGramsLogoMark.png
swift feature_graphic.swift
```

Phone screenshots live in `../screenshots/` (M10's harness), not here — they
were already produced at Play's recommended 1080×1920.
