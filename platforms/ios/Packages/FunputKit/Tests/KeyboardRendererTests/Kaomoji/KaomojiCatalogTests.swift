import CoreText
import Foundation
@testable import KeyboardRenderer
import Testing
#if canImport(UIKit)
import UIKit
#endif

@Suite("Kaomoji catalog")
struct KaomojiCatalogTests {
    @Test("Bundled catalog covers every category")
    func bundledCatalog() {
        let catalog = KaomojiCatalog.bundled
        #expect(catalog.version == "1")
        #expect(catalog.items.count > 150)
        for category in KaomojiCategory.allCases where category != .recent {
            #expect(!catalog.items(in: category).isEmpty)
        }
        #expect(catalog.items.allSatisfy { !$0.text.isEmpty && !$0.name.isEmpty })
        #expect(Set(catalog.items.map(\.text)).count == catalog.items.count)
        #expect(catalog.items.allSatisfy { $0.category != .recent })
    }

    /// Section order is a product decision, not an accident of the enum: recents
    /// lead, and "Yêu thương" sits directly after "Vui vẻ".
    @Test("Category order puts recents first and love next to happy")
    func categoryOrder() {
        let order = KaomojiCategory.allCases
        #expect(order.first == .recent)
        #expect(order.firstIndex(of: .love) == (order.firstIndex(of: .happy).map { $0 + 1 }))
    }

    @Test("Invalid data falls back to an empty catalog")
    func invalidData() {
        let catalog = KaomojiCatalog.decode(data: Data("not-json".utf8))
        #expect(catalog.version.isEmpty)
        #expect(catalog.items.isEmpty)
    }

#if canImport(UIKit)
    /// Kaomoji mix Katakana, Kannada, box drawing and phonetic extensions. Any
    /// character the platform cannot map falls through to the LastResort font and
    /// ships as a tofu box, so the catalog is gated on font coverage.
    @Test("Every character resolves to a real font")
    func fontCoverage() {
        let base = UIFont.systemFont(ofSize: 17) as CTFont
        var missing: [String] = []
        for item in KaomojiCatalog.bundled.items {
            for character in item.text where !Self.isRenderable(character, base: base) {
                missing.append("\(item.text) → \(character) (\(Self.codePoints(character)))")
            }
        }
        #expect(missing.isEmpty, "Kaomoji render as tofu: \(missing.joined(separator: ", "))")
    }

    private static func isRenderable(_ character: Character, base: CTFont) -> Bool {
        let text = String(character) as CFString
        let fallback = CTFontCreateForString(base, text, CFRangeMake(0, CFStringGetLength(text)))
        return (CTFontCopyPostScriptName(fallback) as String) != "LastResort"
    }

    private static func codePoints(_ character: Character) -> String {
        character.unicodeScalars
            .map { String(format: "U+%04X", $0.value) }
            .joined(separator: " ")
    }
#endif
}
