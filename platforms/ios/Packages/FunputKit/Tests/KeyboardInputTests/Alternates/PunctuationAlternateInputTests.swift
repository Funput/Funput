#if os(iOS) && canImport(FunputCore)
@testable import KeyboardInput
import KeyboardLayout
import Testing

@MainActor
@Suite("Punctuation alternate input")
struct PunctuationAlternateInputTests {
    @Test("A symbol from the period palette is typed as-is")
    func typesSymbol() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()
        type("ban ", with: coordinator, into: document)
        coordinator.handleAlternate(symbol("@"), from: periodKey(), writer: document)
        #expect(document.text == "ban @")
    }

    @Test("A symbol ends the word being composed", arguments: [
        KeyboardInputMethod.telex,
        .telexAdvanced,
        .vni,
    ])
    func closesComposition(method: KeyboardInputMethod) {
        let coordinator = KeyboardInputCoordinator(inputMethod: method)
        let document = TestKeyboardWriter()
        type("an", with: coordinator, into: document)
        coordinator.handleAlternate(symbol("?"), from: periodKey(), writer: document)
        #expect(document.text == "an?")
        #expect(coordinator.composer.buffer().isEmpty)
    }

    @Test("A symbol leaves one-shot Shift armed for the next word")
    func keepsOneShotShift() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()
        coordinator.handle(testKey(.shift), writer: document)
        coordinator.handleAlternate(symbol("("), from: periodKey(), writer: document)
        type("a", with: coordinator, into: document)
        #expect(document.text == "(A")
    }

    @Test("English mode types the symbol too")
    func english() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()
        coordinator.toggleLanguage()
        coordinator.handleAlternate(symbol("#"), from: periodKey(), writer: document)
        #expect(document.text == "#")
    }

    @Test("Keys that type nothing ignore a stray alternate")
    func ignoredRoles() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()
        let effects = coordinator.handleAlternate(symbol("@"), from: testKey(.enter), writer: document)
        #expect(document.text.isEmpty)
        #expect(effects == .none)
    }

    private func symbol(_ text: String) -> KeyAlternate {
        KeyAlternate(text: text, shiftedText: text)
    }

    private func periodKey() -> KeySpec {
        KeySpec(
            id: "period",
            label: ".",
            role: .punctuation,
            alternates: PunctuationKeyAlternates.period,
            alternateColumns: PunctuationKeyAlternates.periodColumns
        )
    }
}
#endif
