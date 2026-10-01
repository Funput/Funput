import Foundation
@testable import FunputShared
import Testing

@Suite("Typo kept words store")
struct TypoKeptWordsStoreTests {
    private func withStore(_ body: (TypoKeptWordsStore) -> Void) {
        let suite = "TypoKeptWordsStoreTests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite) ?? .standard
        defer { defaults.removePersistentDomain(forName: suite) }
        body(TypoKeptWordsStore(defaults: defaults))
    }

    @Test("A saved list reads back under the same reset token")
    func roundTrip() {
        let token = UUID()
        withStore { store in
            store.save(["ko", "mk"], resetToken: token)
            #expect(store.load(resetToken: token) == ["ko", "mk"])
        }
    }

    @Test("Resetting the personal lexicon forgets the kept words too")
    func clearedByReset() {
        withStore { store in
            store.save(["ko"], resetToken: nil)
            #expect(store.load(resetToken: UUID()).isEmpty)
        }
    }

    @Test("Only the newest words up to the limit are kept")
    func capped() {
        let words = (0...TypoKeptWordsStore.limit).map { "w\($0)" }
        withStore { store in
            store.save(words, resetToken: nil)
            let loaded = store.load(resetToken: nil)
            #expect(loaded.count == TypoKeptWordsStore.limit)
            #expect(loaded.first == "w1")
        }
    }
}
