#if os(iOS) && canImport(FunputCore)
import FunputEngine
import KeyboardInput
import PersonalSuggestions
import Testing

/// The shipped Vietnamese pairs through the real native engine, on a store that has
/// learned nothing.
@Suite("Shipped Vietnamese predictions")
struct PersonalSuggestionSeedLanguageTests {
    @Test("Off until told, then follows the language and the tone style")
    func followsTheSetting() throws {
        let engine = try #require(PersonalSuggestionEngine.inMemory())
        #expect(engine.query("", after: "cảm").isEmpty)

        #expect(engine.setSeedLanguage(.vietnamese(.modern)))
        #expect(engine.query("", after: "cảm").map(\.text) == ["ơn"])
        #expect(engine.query("", after: "sức").map(\.text) == ["khoẻ"])

        #expect(engine.setSeedLanguage(.vietnamese(.traditional)))
        #expect(engine.query("", after: "sức").map(\.text) == ["khỏe"])

        #expect(engine.setSeedLanguage(.off))
        #expect(engine.query("", after: "cảm").isEmpty)
    }
}
#endif
