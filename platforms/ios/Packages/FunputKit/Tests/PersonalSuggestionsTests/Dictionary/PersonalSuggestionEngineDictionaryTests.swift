#if os(iOS) && canImport(FunputCore)
import Foundation
import PersonalDictionary
import PersonalSuggestions
import Testing

@Suite("The keyboard's side of the dictionary")
struct PersonalSuggestionEngineDictionaryTests {
    @Test("A blocked word stops being learned and offered")
    func blockStopsSuggestions() throws {
        let engine = try #require(PersonalSuggestionEngine.inMemory())
        engine.learn("deadline")
        engine.learn("deadline")
        engine.learn("deadline")
        engine.learn("deadline")
        #expect(engine.query("dead").map(\.text) == ["deadline"])
        #expect(engine.applyEdit(.block, word: "deadline") == .applied)
        #expect(engine.query("dead").isEmpty)
        #expect(!engine.learn("deadline"))
        #expect(engine.applyEdit(.block, word: "deadline") == .unchanged)
    }

    @Test("Edits the app posted are applied and kept")
    func drainsTheAppsEdits() throws {
        let root = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString, isDirectory: true)
        defer { try? FileManager.default.removeItem(at: root) }
        let keyboard = try #require(PersonalSuggestionEngine.open(storeURL: root))
        #expect(PersonalDictionaryInbox.post(.add, word: "funput", storeURL: root))
        #expect(keyboard.query("fun").isEmpty)

        #expect(keyboard.applyPendingEdits() == 1)
        #expect(keyboard.query("fun").map(\.text) == ["funput"])
        #expect(keyboard.applyPendingEdits() == 0)

        let reopened = try #require(PersonalSuggestionEngine.open(storeURL: root))
        #expect(reopened.query("fun").map(\.text) == ["funput"])
    }
}
#endif
