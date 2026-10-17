import KeyboardInput
import KeyboardRenderer
import PersonalSuggestions
import Testing

/// How correction slots and personal completions share the three slots of the bar.
@Suite("Suggestion bar composer")
struct SuggestionBarComposerTests {
    private func personal(_ words: String...) -> [KeyboardSuggestionCandidate] {
        words.map { KeyboardSuggestionCandidate(text: $0, generation: 3) }
    }

    @Test("Without a correction the bar is the personal list, untouched")
    func personalOnly() {
        let composed = SuggestionBarComposer.compose(
            correction: nil, personal: personal("không", "khi", "khá"), generation: 3
        )
        #expect(composed == personal("không", "khi", "khá"))
    }

    @Test("An offer puts the typed word first, its repair next, and completions after")
    func offered() {
        let composed = SuggestionBarComposer.compose(
            correction: .offered(typed: "đườnh", candidates: ["đường"]),
            personal: personal("đưa", "đứng"),
            generation: 3
        )
        #expect(composed.map(\.kind) == [.correctionTyped, .correctionCandidate, .suggestion])
        #expect(composed.map(\.text) == ["đườnh", "đường", "đưa"])
        #expect(composed.allSatisfy { $0.generation == 3 })
    }

    @Test("Two repairs fill the bar on their own")
    func ambiguous() {
        let composed = SuggestionBarComposer.compose(
            correction: .offered(typed: "nhad", candidates: ["nhà", "nhá"]),
            personal: personal("nhanh"),
            generation: 1
        )
        #expect(composed.map(\.text) == ["nhad", "nhà", "nhá"])
    }

    @Test("After a repair the undo chip leads and completions follow")
    func applied() {
        let composed = SuggestionBarComposer.compose(
            correction: .applied(undoText: "đườnh"), personal: personal("đi", "đến"), generation: 3
        )
        #expect(composed.map(\.kind) == [.correctionUndo, .suggestion, .suggestion])
        #expect(composed.first?.text == "đườnh")
    }

    @Test("A completion that repeats a correction slot is dropped")
    func deduplicated() {
        let composed = SuggestionBarComposer.compose(
            correction: .offered(typed: "đườnh", candidates: ["đường"]),
            personal: personal("đường", "đưa"),
            generation: 3
        )
        #expect(composed.map(\.text) == ["đườnh", "đường", "đưa"])
        #expect(composed[1].kind == .correctionCandidate)
    }

    @Test("A repair parked after Space is drawn exactly like one offered before it")
    func parked() {
        let composed = SuggestionBarComposer.compose(
            correction: .parked(typed: "nhad", candidates: ["nhà", "nhá"]),
            personal: personal("nhanh"),
            generation: 2
        )
        #expect(composed.map(\.kind) == [.correctionTyped, .correctionCandidate, .correctionCandidate])
        #expect(composed.map(\.text) == ["nhad", "nhà", "nhá"])
    }

    @Test("A correction shows with personal suggestions switched off")
    func withoutPersonal() {
        let composed = SuggestionBarComposer.compose(
            correction: .offered(typed: "đườnh", candidates: ["đường"]), personal: [], generation: 0
        )
        #expect(composed.map(\.text) == ["đườnh", "đường"])
    }
}
