#if os(iOS) && canImport(FunputCore)
import Foundation
import Testing

@testable import PersonalSuggestions

@MainActor
@Suite("Correction word source")
struct CorrectionWordSourceTests {
    @Test("A kept word is recognized in any case, and handed on to be stored")
    func keeps() {
        let source = CorrectionWordSource(lexiconURL: { nil })
        var stored: [String] = []
        source.onKeep = { stored = $0 }

        source.keep("Ko")
        source.keep("ko")

        #expect(stored == ["ko"], "the same word twice is one word")
        #expect(source.isKept("KO"))
        #expect(!source.isKept("ki"))
    }

    @Test("The oldest kept word goes once the list is full")
    func capped() {
        let source = CorrectionWordSource(lexiconURL: { nil })
        var stored: [String] = []
        source.onKeep = { stored = $0 }

        for index in 0...CorrectionWordSource.keptLimit {
            source.keep("w\(index)")
        }

        #expect(stored.count == CorrectionWordSource.keptLimit)
        #expect(stored.first == "w1")
        #expect(!source.isKept("w0"), "dropped from the lookup too, not just the list")
    }

    @Test("Words kept in an earlier session are recognized again")
    func restored() {
        let source = CorrectionWordSource(lexiconURL: { nil })
        source.restoreKept(["mk"])
        #expect(source.isKept("Mk"))
    }

    @Test("One sighting weighs, two make the word the user's own")
    func counts() throws {
        // Raw keys, so the shipped syllable list adds nothing to the count.
        let source = CorrectionWordSource(engine: try #require(PersonalSuggestionEngine.inMemory()))
        #expect(source.weight("dduwowfnh") == 0)

        source.learn("dduwowfnh")
        #expect(source.weight("dduwowfnh") == 1)
        #expect(!source.recognizes("dduwowfnh"), "a typo left alone looks exactly like this")

        source.learn("Dduwowfnh")
        #expect(source.weight("dduwowfnh") == 2)
        #expect(source.recognizes("dduwowfnh"))
        #expect(source.weight("dduwowfnj") == 0, "only the word that was typed")
    }

    @Test("Counting needs no word list, and the engine is never written to")
    func countsBeforeReady() throws {
        let source = CorrectionWordSource(lexiconURL: { nil })
        source.learn("dduwowfnh")
        #expect(source.weight("dduwowfnh") == 1)

        let engine = try #require(PersonalSuggestionEngine.inMemory())
        let ready = CorrectionWordSource(engine: engine)
        ready.learn("dduwowfnh")
        ready.learn("dduwowfnh")
        #expect(!engine.recognizes("dduwowfnh"))
    }

    @Test("A shipped syllable weighs by its band, on top of the user's own count")
    func shippedWeight() throws {
        let source = CorrectionWordSource(engine: try #require(PersonalSuggestionEngine.inMemory()))
        #expect(source.weight("là") == 4, "the most common band")
        #expect(source.weight("Là") == 4, "case does not matter")
        #expect(source.weight("dduwowfnh") == 0, "raw keys are on no list")

        source.learn("là")

        #expect(source.weight("là") == 5)
    }

    @Test("The rarest word goes once the counts are full")
    func cappedCounts() throws {
        let source = CorrectionWordSource(engine: try #require(PersonalSuggestionEngine.inMemory()))
        source.learn("w0")
        source.learn("w0")
        for index in 1...CorrectionWordSource.countLimit {
            source.learn("w\(index)")
        }
        #expect(source.weight("w0") == 2, "the one seen twice stays")
        #expect(source.personalCounts.count == CorrectionWordSource.countLimit)
    }
}
#endif
