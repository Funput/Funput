#if os(iOS) && canImport(FunputCore)
import Foundation
import PersonalSuggestions
import Testing

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
}
#endif
