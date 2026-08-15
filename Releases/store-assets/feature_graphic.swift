// Play Store feature graphic (1024x500) — cream gradient field, the
// CoffeeGrams brand mark (composited from CoffeeGramsLogoMark.png, the same
// transparent-field export used for the in-app header lockup), the
// wordmark, and a short tagline. Palette matches DESIGN.md's tokens exactly
// (not render.swift's icon-tuned constants — this is UI chrome, not the
// icon asset).
//
// Needs CoffeeGramsLogoMark.png (1024x1024, transparent) alongside this
// script — pull it from the iOS repo's coffeegrams_logo/ directory, the
// same source the Android adaptive icon (ic_launcher_foreground.xml) was
// transliterated from for pixel parity:
//   gh api repos/JayReece313/coffeegrams/contents/coffeegrams_logo/CoffeeGramsLogoMark.png \
//     --jq '.content' | base64 -d > CoffeeGramsLogoMark.png
//
// Run: swift feature_graphic.swift
import CoreGraphics
import ImageIO
import Foundation
import UniformTypeIdentifiers
import CoreText

let W = 1024, H = 500
let cs = CGColorSpaceCreateDeviceRGB()
func rgb(_ hex: UInt32) -> CGColor {
    let r = CGFloat((hex >> 16) & 0xFF) / 255
    let g = CGFloat((hex >> 8) & 0xFF) / 255
    let b = CGFloat(hex & 0xFF) / 255
    return CGColor(colorSpace: cs, components: [r, g, b, 1])!
}
// DESIGN.md light tokens
let background = rgb(0xF4EADB)
let surface = rgb(0xFBF4E9)
let textPrimary = rgb(0x3A2A1E)
let textSecondary = rgb(0x6B5647)
let accent = rgb(0xC6852E)

let ctx = CGContext(data: nil, width: W, height: H, bitsPerComponent: 8, bytesPerRow: 0,
                     space: cs, bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!

// Background: warm cream gradient, Surface -> Background, left to right.
let bg = CGGradient(colorsSpace: cs, colors: [surface, background] as CFArray, locations: [0, 1])!
ctx.drawLinearGradient(bg, start: CGPoint(x: 0, y: 0), end: CGPoint(x: CGFloat(W), y: 0), options: [])

// Logo mark, loaded from the transparent-field PNG already used for the
// in-app header lockup — draw it into a square on the left third.
let markSize: CGFloat = 380
let markPath = FileManager.default.currentDirectoryPath + "/CoffeeGramsLogoMark.png"
if let dataProvider = CGDataProvider(filename: markPath),
   let markImage = CGImage(pngDataProviderSource: dataProvider, decode: nil, shouldInterpolate: true, intent: .defaultIntent) {
    let markRect = CGRect(x: 70, y: (CGFloat(H) - markSize) / 2, width: markSize, height: markSize)
    ctx.draw(markImage, in: markRect)
} else {
    FileHandle.standardError.write("could not load CoffeeGramsLogoMark.png from \(markPath)\n".data(using: .utf8)!)
    exit(1)
}

// Wordmark + tagline, right of the mark.
func drawText(_ text: String, font: CTFont, color: CGColor, x: CGFloat, y: CGFloat) {
    let attrs: [CFString: Any] = [kCTFontAttributeName: font, kCTForegroundColorAttributeName: color]
    let attrString = CFAttributedStringCreate(nil, text as CFString, attrs as CFDictionary)!
    let line = CTLineCreateWithAttributedString(attrString)
    ctx.textPosition = CGPoint(x: x, y: y)
    CTLineDraw(line, ctx)
}

let textX: CGFloat = 70 + markSize + 40
let rightMargin: CGFloat = 50
let maxTextWidth = CGFloat(W) - textX - rightMargin

// Shrink the wordmark to fit the available width right of the mark, rather
// than hard-coding a size and clipping at the canvas edge.
func fittedFont(_ name: String, text: String, startSize: CGFloat, maxWidth: CGFloat) -> CTFont {
    var size = startSize
    while size > 20 {
        let font = CTFontCreateWithName(name as CFString, size, nil)
        let attrString = CFAttributedStringCreate(nil, text as CFString, [kCTFontAttributeName: font] as CFDictionary)!
        let line = CTLineCreateWithAttributedString(attrString)
        if CTLineGetTypographicBounds(line, nil, nil, nil) <= Double(maxWidth) {
            return font
        }
        size -= 2
    }
    return CTFontCreateWithName(name as CFString, 20, nil)
}

let wordmarkFont = fittedFont("AvenirNext-Bold", text: "CoffeeGrams", startSize: 96, maxWidth: maxTextWidth)
let taglineFont = fittedFont("AvenirNext-DemiBold", text: "Brew Calculator & Guided Timer", startSize: 34, maxWidth: maxTextWidth)

drawText("CoffeeGrams", font: wordmarkFont, color: textPrimary, x: textX, y: 268)
drawText("Brew Calculator & Guided Timer", font: taglineFont, color: textSecondary, x: textX, y: 200)

// A thin accent rule under the tagline, echoing the app's 10% caramel accent.
ctx.setFillColor(accent)
ctx.fill(CGRect(x: textX, y: 168, width: 90, height: 6))

guard let image = ctx.makeImage() else { fatalError("no image") }
let outPath = FileManager.default.currentDirectoryPath + "/feature-graphic-1024x500.png"
let dest = CGImageDestinationCreateWithURL(URL(fileURLWithPath: outPath) as CFURL, UTType.png.identifier as CFString, 1, nil)!
CGImageDestinationAddImage(dest, image, nil)
CGImageDestinationFinalize(dest)
print("wrote \(outPath)")
