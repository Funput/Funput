#if os(iOS) && canImport(FunputCore)
@testable import KeyboardInput
import FunputShared
import KeyboardLayout
import Testing

@MainActor
@Suite("Return to letters after punctuation")
struct LetterPageReturnTests {
    @Test("A space after punctuation on a symbol page brings back the letters", arguments: [
        KeyRole.symbols,
        .moreSymbols,
    ])
    func returns(page: KeyRole) {
        let (coordinator, document) = subject()
        type("chao", with: coordinator, into: document)
        coordinator.handle(testKey(.symbols), writer: document)
        if page == .moreSymbols { coordinator.handle(testKey(.moreSymbols), writer: document) }
        type("!", role: .punctuation, with: coordinator, into: document)
        let effects = coordinator.handle(testKey(.space, label: " "), writer: document)

        #expect(document.text == "chao! ")
        #expect(coordinator.state.layoutMode == .letters)
        #expect(effects.presentationChanged)
    }

    @Test("The next sentence starts capitalized on the letters page")
    func capitalizesNextSentence() {
        let (coordinator, document) = subject()
        coordinator.updateContext(inputContext(
            editorMode: .text,
            enterAction: .newLine,
            autocapitalization: .sentences
        ))
        type("Chao", with: coordinator, into: document)
        coordinator.handle(testKey(.symbols), writer: document)
        type("?", role: .punctuation, with: coordinator, into: document)
        coordinator.handle(testKey(.space, label: " "), writer: document)

        #expect(coordinator.state.layoutMode == .letters)
        #expect(coordinator.state.shiftState == .uppercase)
    }

    @Test("A symbol chosen from the period palette counts as punctuation")
    func paletteSymbol() {
        let (coordinator, document) = subject()
        coordinator.handle(testKey(.symbols), writer: document)
        let period = KeySpec(id: "period", label: ".", role: .punctuation)
        coordinator.handleAlternate(KeyAlternate(text: ";"), from: period, writer: document)
        coordinator.handle(testKey(.space, label: " "), writer: document)

        #expect(coordinator.state.layoutMode == .letters)
    }

    @Test("Numbers and a bare switch keep the symbol page", arguments: ["10", ""])
    func staysForNumbers(typed: String) {
        let (coordinator, document) = subject()
        type("chao", with: coordinator, into: document)
        coordinator.handle(testKey(.symbols), writer: document)
        type(typed, role: .punctuation, with: coordinator, into: document)
        coordinator.handle(testKey(.space, label: " "), writer: document)

        #expect(coordinator.state.layoutMode == .symbolsPrimary)
    }

    @Test("Turning the setting off keeps the symbol page")
    func disabled() {
        let (coordinator, document) = subject()
        var configuration = FunputConfiguration.default
        configuration.returnsToLettersAfterPunctuation = false
        coordinator.apply(configuration)
        coordinator.handle(testKey(.symbols), writer: document)
        type("!", role: .punctuation, with: coordinator, into: document)
        coordinator.handle(testKey(.space, label: " "), writer: document)

        #expect(!coordinator.returnsToLettersAfterPunctuation)
        #expect(coordinator.state.layoutMode == .symbolsPrimary)
    }

    private func subject() -> (KeyboardInputCoordinator, TestKeyboardWriter) {
        (KeyboardInputCoordinator(inputMethod: .telex), TestKeyboardWriter())
    }
}
#endif
