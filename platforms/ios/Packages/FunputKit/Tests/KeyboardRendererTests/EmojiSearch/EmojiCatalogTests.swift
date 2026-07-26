import Foundation
@testable import KeyboardRenderer
import Testing

@Suite("Emoji catalog")
struct EmojiCatalogTests {
    @Test("Bundled Unicode catalog contains every supported category")
    func bundledCatalog() {
        let catalog = EmojiCatalog.bundled
        #expect(catalog.version == "15.1")
        #expect(catalog.annotationVersion == "48.2")
        #expect(catalog.emojis.count > 1_500)
        for category in EmojiCategory.allCases where category != .recent {
            #expect(!catalog.items(in: category).isEmpty)
        }
        #expect(catalog.emojis.allSatisfy { !$0.glyph.isEmpty && !$0.name.isEmpty })
        #expect(catalog.emojis.contains { $0.localizedName != nil && !$0.searchTerms.isEmpty })
        #expect(Set(catalog.emojis.map(\.glyph)).count == catalog.emojis.count)
        let index = EmojiSearchIndex(catalog: catalog)
        #expect(!index.search("mặt cười").isEmpty)
        #expect(!index.search("mat cuoi").isEmpty)
        #expect(index.search("cho").contains { $0.glyph == "🐕" })
    }

    @Test("Legacy schema decodes with empty localization metadata")
    func legacyData() throws {
        let data = Data(
            #"{"version":"test","emojis":[{"glyph":"😀","name":"face","category":"smileys_people"}]}"#
                .utf8
        )
        let catalog = try JSONDecoder().decode(EmojiCatalog.self, from: data)
        #expect(catalog.annotationVersion.isEmpty)
        #expect(catalog.emojis[0].localizedName == nil)
        #expect(catalog.emojis[0].searchTerms.isEmpty)
    }

    @Test("Invalid data falls back to an empty catalog")
    func invalidData() {
        let catalog = EmojiCatalog.decode(data: Data("not-json".utf8))
        #expect(catalog.version.isEmpty)
        #expect(catalog.emojis.isEmpty)
    }
}
