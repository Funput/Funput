#if canImport(UIKit)
import KeyboardRenderer
import Testing

@MainActor
@Suite("Emoji catalog cache")
struct EmojiCatalogCacheTests {
    @Test("Decodes once until released, then again")
    func decodesOncePerRelease() {
        var decodes = 0
        let cache = EmojiCatalogCache {
            decodes += 1
            return EmojiCatalog(version: "\(decodes)", emojis: [])
        }
        #expect(cache.load().version == "1")
        #expect(cache.load().version == "1")
        #expect(decodes == 1)

        cache.release()
        #expect(cache.load().version == "2")
        #expect(decodes == 2)
    }
}
#endif
