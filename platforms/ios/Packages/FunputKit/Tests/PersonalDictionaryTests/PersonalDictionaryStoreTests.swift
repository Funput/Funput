#if os(iOS) && canImport(FunputCore)
import Foundation
import FunputCore
import PersonalDictionary
import Testing

@Suite("Personal dictionary seen from the app")
struct PersonalDictionaryStoreTests {
    private let root = FileManager.default.temporaryDirectory
        .appendingPathComponent(UUID().uuidString, isDirectory: true)

    @Test("The word limit is the engine's own")
    func limitMatchesTheEngine() {
        #expect(DictionaryLimits.maxWordLength == Int(SUGGESTION_CHARS_CAP))
    }

    @Test("Reading a store that does not exist yet writes nothing")
    func readingWritesNothing() async throws {
        let store = PersonalDictionaryStore(storeURL: root)
        let page = try await store.load(DictionaryQuery())
        #expect(page.words.isEmpty)
        #expect(!FileManager.default.fileExists(atPath: root.path))
    }

    @Test("An applied edit is posted and shows on the next load")
    func appliedEditsShowAtOnce() async throws {
        defer { try? FileManager.default.removeItem(at: root) }
        let store = PersonalDictionaryStore(storeURL: root)
        #expect(try await store.apply(.add, word: "Funput") == .applied)
        #expect(try await store.apply(.block, word: "đm") == .applied)

        let added = try await store.load(DictionaryQuery(filter: .added))
        #expect(added.words.map(\.text) == ["funput"])
        #expect(added.words.first?.origin == .added)
        #expect(added.count(of: .added) == 1)
        #expect(added.count(of: .blocked) == 1)

        let blocked = try await store.load(DictionaryQuery(filter: .blocked, search: "DM"))
        #expect(blocked.words.map(\.text) == ["đm"])
        let inbox = root.appendingPathComponent("edits")
        #expect(try FileManager.default.contentsOfDirectory(atPath: inbox.path).count == 2)
    }

    @Test("A refused edit is reported and never posted")
    func refusedEditsStayLocal() async throws {
        defer { try? FileManager.default.removeItem(at: root) }
        let store = PersonalDictionaryStore(storeURL: root)
        #expect(try await store.apply(.add, word: "hai từ") == .invalid)
        #expect(try await store.apply(.forget, word: "chưa có") == .invalid)
        #expect(try await store.apply(.forget, word: "chưacó") == .unchanged)
        #expect(!FileManager.default.fileExists(atPath: root.path))
    }

    @Test("Search ignores marks, and alphabetical order ignores them first")
    func searchAndSort() async throws {
        defer { try? FileManager.default.removeItem(at: root) }
        let store = PersonalDictionaryStore(storeURL: root)
        for word in ["zalo", "ăn", "được", "an"] {
            #expect(try await store.apply(.add, word: word) == .applied)
        }
        let found = try await store.load(DictionaryQuery(filter: .added, search: "duoc"))
        #expect(found.words.map(\.text) == ["được"])

        let sorted = try await store.load(DictionaryQuery(filter: .added, sort: .alphabetical))
        #expect(sorted.words.map(\.text) == ["an", "ăn", "được", "zalo"])
    }
}
#endif
