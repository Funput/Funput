#if os(iOS) && canImport(FunputCore)
import PersonalSuggestions
import Testing

/// Two words of context through the real native engine.
@Suite("Trigram predictions")
struct PersonalSuggestionTrigramTests {
    @Test("Two words answer where the word before alone says something else")
    func twoWordsOfContext() throws {
        let engine = try #require(PersonalSuggestionEngine.inMemory())
        for _ in 0..<10 { typed(["nhờ", "ơn", "trời"], into: engine) }
        for _ in 0..<3 { typed(["cảm", "ơn", "bạn"], into: engine) }

        #expect(engine.query("", after: "ơn").map(\.text) == ["trời"])
        #expect(engine.query("", after: "cảm ơn").map(\.text) == ["bạn"])
        #expect(engine.query("", after: "mới ơn").map(\.text) == ["trời"], "no evidence steps down")
    }

    private func typed(_ words: [String], into engine: PersonalSuggestionEngine) {
        var previous: String?
        for word in words {
            engine.learn(word, after: previous)
            previous = word
        }
    }
}
#endif
