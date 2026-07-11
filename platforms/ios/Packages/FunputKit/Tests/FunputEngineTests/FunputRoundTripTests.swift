#if canImport(FunputCore)
import FunputEngine
import Testing

@MainActor
struct FunputRoundTripTests {
    @Test("TELEX round trips through the Swift wrapper")
    func telex() {
        let cases = [
            ("as", "á"),
            ("dd", "đ"),
            ("xins chaof", "xín chào"),
        ]
        for (input, expected) in cases {
            #expect(CompositionSimulator.type(input, method: .telex) == expected)
        }
    }

    @Test("VNI round trips through the Swift wrapper")
    func vni() {
        let cases = [
            ("a1", "á"),
            ("d9", "đ"),
            ("ma1 ca2", "má cà"),
        ]
        for (input, expected) in cases {
            #expect(CompositionSimulator.type(input, method: .vni) == expected)
        }
    }

    @Test("English restore behavior matches the Rust contract")
    func englishRestore() {
        let cases = [
            ("card ", "card "),
            ("cool ", "cool "),
            ("mas ", "má "),
        ]
        for (input, expected) in cases {
            #expect(CompositionSimulator.type(input, method: .telex) == expected)
        }
    }

    @Test("Typed result exposes delete and output fields")
    func resultFields() {
        let composer = FunputComposer()
        composer.setInputMethod(.telex)

        #expect(composer.process("a").action == .none)
        #expect(composer.process("s") == FunputCompositionResult(
            action: .send,
            deleteCount: 1,
            text: "á"
        ))
    }
}
#endif
