import Foundation
import PersonalSuggestions
import Testing

/// Expected orders follow the ranks in the committed `en.tsv` (which 32, when 40, what 43);
/// a data refresh that reorders them must update these tests with it.
struct EnglishLexiconBridgeTests {
    @Test func bundledLexiconCompletesAndDeduplicates() throws {
        let engine = try #require(PersonalSuggestionEngine.inMemory())
        #expect(engine.attachLexicon(url: try LexiconTestResource.url()))
        #expect(engine.query("wh").map(\.text) == ["which", "when", "what"])
        #expect(engine.query("ip").map(\.text).contains("iPhone"))
        #expect(engine.query("").isEmpty)
        #expect(engine.query("thà").isEmpty)
        #expect(engine.learn("iphone"))
        #expect(engine.learn("iphone"))
        let values = engine.query("ip").map(\.text)
        #expect(values.first == "iphone")
        #expect(values.filter { $0.lowercased() == "iphone" }.count == 1)
    }

    @Test func attachFailureKeepsLexiconAndPersonalWords() throws {
        let engine = try #require(PersonalSuggestionEngine.inMemory())
        #expect(engine.attachLexicon(url: try LexiconTestResource.url()))
        let root = FileManager.default.temporaryDirectory.appendingPathComponent("Từ điển-\(UUID())")
        defer { try? FileManager.default.removeItem(at: root) }
        #expect(!engine.attachLexicon(url: root))
        try Data("corrupt".utf8).write(to: root)
        #expect(!engine.attachLexicon(url: root))
        #expect(engine.query("wh").map(\.text) == ["which", "when", "what"])
        #expect(engine.learn("whimsy"))
        #expect(engine.learn("whimsy"))
        #expect(engine.query("wh").first?.text == "whimsy")
    }

    @Test func persistentResetKeepsDictionary() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: root) }
        let engine = try #require(PersonalSuggestionEngine.open(storeURL: root))
        #expect(engine.attachLexicon(url: try LexiconTestResource.url()))
        #expect(engine.learn("whimsy"))
        #expect(engine.learn("whimsy"))
        #expect(engine.reset())
        #expect(engine.stats().words == 0)
        #expect(engine.query("wh").map(\.text) == ["which", "when", "what"])
    }

    @Test func vietnameseStoreYieldsOnlyWithMarkedCandidates() throws {
        let engine = try #require(PersonalSuggestionEngine.inMemory())
        #expect(engine.attachLexicon(url: try LexiconTestResource.url()))
        #expect(engine.learn("ăn"))
        #expect(engine.learn("ăn"))
        #expect(engine.query("an").count == 3)
        // 200 is the default lexicon_yield_after_words; retuning it must update this test.
        for index in 0..<200 {
            // Distinct alphabetic marked words without depending on a user's store.
            let suffix = String(UnicodeScalar(97 + index / 26)!) + String(UnicodeScalar(97 + index % 26)!)
            #expect(engine.learn("đ" + suffix))
            #expect(engine.learn("đ" + suffix))
        }
        #expect(engine.query("an").map(\.text) == ["ăn"])
        #expect(engine.query("wh").count == 3)
        #expect(engine.reset())
        #expect(engine.query("an").count == 3)
    }
}
