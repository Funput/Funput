import Foundation
@testable import FunputShared
import Testing

@Suite("Emoji recents store")
struct EmojiRecentsStoreTests {
    @Test("Records most-recent-first without duplicates")
    func orderingAndDeduplication() {
        withStore { store, _ in
            _ = store.record(item("😀"))
            _ = store.record(item("🎉"))
            let result = store.record(item("😀"))
            #expect(result.map(\.glyph) == ["😀", "🎉"])
            #expect(store.load() == result)
        }
    }

    @Test("Limits persisted recents")
    func limit() {
        withStore { store, _ in
            for index in 0...EmojiRecentsStore.limit {
                _ = store.record(item("emoji-\(index)"))
            }
            #expect(EmojiRecentsStore.limit == 8)
            #expect(store.load().count == 8)
            #expect(store.load().first?.glyph == "emoji-8")
        }
    }

    @Test("Existing history is limited to the newest eight entries")
    func legacyHistory() throws {
        let data = try JSONEncoder().encode((0..<30).map { item("emoji-\($0)") })
        withStore { store, defaults in
            defaults.set(data, forKey: FunputAppGroup.emojiRecentsKey)
            #expect(store.load().map(\.glyph) == (0..<8).map { "emoji-\($0)" })
        }
    }

    @Test("Corrupt data returns an empty list")
    func corruptData() {
        withStore { store, defaults in
            defaults.set(Data([0, 1]), forKey: FunputAppGroup.emojiRecentsKey)
            #expect(store.load().isEmpty)
        }
    }

    private func item(_ glyph: String) -> EmojiRecent {
        EmojiRecent(glyph: glyph, name: glyph, category: "symbols")
    }

    private func withStore(_ body: (EmojiRecentsStore, UserDefaults) -> Void) {
        let name = "EmojiRecentsStoreTests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: name)!
        defer { defaults.removePersistentDomain(forName: name) }
        body(EmojiRecentsStore(defaults: defaults), defaults)
    }
}
