import PersonalDictionary
import Testing
@testable import Funput

@MainActor
@Suite("Dictionary page model")
struct PersonalDictionaryModelTests {
    private func loaded(_ store: any PersonalDictionaryStoring = DictionaryPreviewStore()) async
        -> PersonalDictionaryModel {
        let model = PersonalDictionaryModel(store: store)
        await model.reload()
        return model
    }

    @Test("A load fills the list and counts all three lists")
    func loadCountsEveryList() async {
        let model = await loaded()
        #expect(model.hasLoaded)
        #expect(model.page.words.first?.text == "được")
        #expect(model.page.count(of: .learned) == DictionaryPreviewStore.samples.count - 1)
        #expect(model.page.count(of: .added) == 1)
        #expect(model.page.count(of: .blocked) == 1)
        #expect(model.highestUses == 214)
    }

    @Test("Blocking moves a word to the blocked list, and unblocking lifts it")
    func blockAndUnblock() async throws {
        let model = await loaded()
        let word = try #require(model.page.words.first { $0.text == "deadline" })
        await model.block(word)
        #expect(!model.page.words.contains(word))
        #expect(model.page.count(of: .blocked) == 2)

        model.filter = .blocked
        await model.reload()
        let blocked = try #require(model.page.words.first { $0.text == "deadline" })
        await model.unblock(blocked)
        #expect(model.page.count(of: .blocked) == 1)
    }

    @Test("Adding reports what the engine said")
    func addReportsTheOutcome() async {
        let model = await loaded()
        #expect(await model.apply(.add, to: "Funput") == .unchanged)
        #expect(await model.apply(.add, to: "hai từ") == .invalid)
        #expect(await model.apply(.add, to: "Shopee") == .applied)
        #expect(model.page.count(of: .added) == 2)
    }

    @Test("A store that cannot be reached shows its error, and an edit says so once")
    func failuresSurface() async {
        let model = await loaded(FailingDictionaryStore())
        #expect(model.loadError == PersonalDictionaryError.unreadable.errorDescription)
        #expect(await model.apply(.block, to: "x") == nil)
        #expect(model.actionError == PersonalDictionaryError.unwritable.errorDescription)
    }

    @Test("Search waits for typing to pause, then asks once")
    func searchIsDebounced() async throws {
        let model = await loaded()
        model.search = "d"
        model.search = "du"
        model.search = "duoc"
        #expect(model.page.words.count > 1)
        try await Task.sleep(for: PersonalDictionaryModel.searchPause * 4)
        #expect(model.page.words.map(\.text) == ["được"])
    }
}

private struct FailingDictionaryStore: PersonalDictionaryStoring {
    func load(_ query: DictionaryQuery) async throws -> DictionaryPage {
        throw PersonalDictionaryError.unreadable
    }

    func apply(_ kind: DictionaryEditKind, word: String) async throws -> DictionaryEditOutcome {
        throw PersonalDictionaryError.unwritable
    }
}
