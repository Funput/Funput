import Foundation
@testable import KeyboardRenderer
import Testing

@Suite("Emoji catalog")
struct EmojiCatalogTests {
    @Test("Bundled Unicode catalog contains every supported category")
    func bundledCatalog() {
        let catalog = EmojiCatalog.bundled
        #expect(catalog.version == "15.1")
        #expect(catalog.emojis.count > 1_500)
        for category in EmojiCategory.allCases where category != .recent {
            #expect(!catalog.items(in: category).isEmpty)
        }
        #expect(catalog.emojis.allSatisfy { !$0.glyph.isEmpty && !$0.name.isEmpty })
        #expect(Set(catalog.emojis.map(\.glyph)).count == catalog.emojis.count)
    }

    @Test("Invalid data falls back to an empty catalog")
    func invalidData() {
        let catalog = EmojiCatalog.decode(data: Data("not-json".utf8))
        #expect(catalog.version.isEmpty)
        #expect(catalog.emojis.isEmpty)
    }
}
