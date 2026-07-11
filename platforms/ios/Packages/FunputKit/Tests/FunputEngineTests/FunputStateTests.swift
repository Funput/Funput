#if canImport(FunputCore)
import FunputEngine
import Testing

@MainActor
struct FunputStateTests {
    @Test("Backspace updates the active composition")
    func backspace() {
        let composer = FunputComposer()
        composer.setInputMethod(.telex)
        for scalar in "Phua".unicodeScalars {
            composer.process(scalar)
        }

        #expect(composer.backspace().action == .none)
        let result = composer.process("s")
        #expect(result.action == .send)
        #expect(result.text == "ú")
    }

    @Test("Buffer reflects composition and clear resets it")
    func buffer() {
        let composer = FunputComposer()
        composer.setInputMethod(.vni)

        composer.process("a")
        #expect(composer.buffer() == "a")
        composer.process("1")
        #expect(composer.buffer() == "á")
        composer.clear()
        #expect(composer.buffer().isEmpty)
    }

    @Test("Unicode shortcuts cross the C boundary safely")
    func shortcuts() {
        #expect(CompositionSimulator.type("vn ", method: .telex) { composer in
            composer.addShortcut(trigger: "vn", expansion: "Việt Nam")
        } == "Việt Nam ")
    }

    @Test("Clearing shortcuts disables expansion")
    func clearShortcuts() {
        let composer = FunputComposer()
        composer.setInputMethod(.telex)
        composer.addShortcut(trigger: "vn", expansion: "Việt Nam")
        composer.clearShortcuts()
        composer.process("v")
        composer.process("n")
        #expect(composer.process(" ").action == .none)
    }
}
#endif
