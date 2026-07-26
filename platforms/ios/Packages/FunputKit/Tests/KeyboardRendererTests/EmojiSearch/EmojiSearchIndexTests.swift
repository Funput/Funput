@testable import KeyboardRenderer
import Testing

@Suite("Emoji search index")
struct EmojiSearchIndexTests {
    let face = EmojiItem(
        glyph: "😀",
        name: "grinning face",
        localizedName: "mặt cười",
        searchTerms: ["happy", "vui vẻ"],
        category: .smileysPeople
    )
    let dog = EmojiItem(
        glyph: "🐕",
        name: "dog",
        localizedName: "chó",
        searchTerms: ["pet", "thú cưng"],
        category: .animalsNature
    )

    @Test("Vietnamese search is case and accent insensitive")
    func vietnameseNormalization() {
        let index = makeIndex()
        #expect(index.search("MẶT CƯỜI").first == face)
        #expect(index.search("mat cuoi").first == face)
        #expect(index.search("cho").first == dog)
    }

    @Test("English names and multilingual keywords match")
    func multilingualTerms() {
        let index = makeIndex()
        #expect(index.search("grinning").first == face)
        #expect(index.search("vui ve").first == face)
        #expect(index.search("thu cung").first == dog)
    }

    @Test("Every query token must match")
    func multipleTokens() {
        let index = makeIndex()
        #expect(index.search("mat vui").first == face)
        #expect(index.search("mat cho").isEmpty)
    }

    @Test("Ranking is stable and respects the limit")
    func rankingAndLimit() {
        let prefix = EmojiItem(
            glyph: "🙂", name: "grinning face softly", category: .smileysPeople
        )
        let catalog = EmojiCatalog(version: "test", emojis: [prefix, face, dog])
        let results = EmojiSearchIndex(catalog: catalog).search("grinning face", limit: 1)
        #expect(results == [face])
    }

    private func makeIndex() -> EmojiSearchIndex {
        EmojiSearchIndex(catalog: EmojiCatalog(version: "test", emojis: [face, dog]))
    }
}
