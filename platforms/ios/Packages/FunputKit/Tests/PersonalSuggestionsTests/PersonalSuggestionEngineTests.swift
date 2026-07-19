#if os(iOS) && canImport(FunputCore)
import Foundation
import PersonalSuggestions
import Testing

@Suite("Personal suggestion bridge")
struct PersonalSuggestionEngineTests {
    @Test("Learns and queries UTF-32 candidates")
    func learnsAndQueries() throws {
        let engine = try #require(PersonalSuggestionEngine.inMemory())
        #expect(engine.learn("không"))
        #expect(engine.learn("không"))
        #expect(engine.query("kh").map(\.text) == ["không"])
        #expect(engine.stats().promotedWords == 1)
    }

    @Test("Persistent bridge round trips and resets")
    func persistentRoundTrip() throws {
        let root = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString, isDirectory: true)
        defer { try? FileManager.default.removeItem(at: root) }
        do {
            let engine = try #require(PersonalSuggestionEngine.open(storeURL: root))
            #expect(engine.learn("chào"))
            #expect(engine.learn("chào"))
            #expect(engine.flush())
        }
        let reopened = try #require(PersonalSuggestionEngine.open(storeURL: root))
        #expect(reopened.query("ch").map(\.text) == ["chào"])
        #expect(reopened.reset())
        #expect(reopened.query("ch").isEmpty)
    }

    @Test("Invalid and empty inputs are neutral")
    func neutralInputs() throws {
        let engine = try #require(PersonalSuggestionEngine.inMemory())
        #expect(!engine.learn(""))
        #expect(engine.query("").isEmpty)
        #expect(engine.query(String(repeating: "a", count: 33)).isEmpty)
    }
}
#endif
