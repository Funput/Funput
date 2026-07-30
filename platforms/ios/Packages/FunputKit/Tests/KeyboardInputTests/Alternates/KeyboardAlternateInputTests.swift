#if os(iOS) && canImport(FunputCore)
@testable import KeyboardInput
import KeyboardLayout
import Testing

@MainActor
@Suite("Alternate character input")
struct KeyboardAlternateInputTests {
    @Test("Precomposed alternate stays inside Vietnamese composition", arguments: [
        KeyboardInputMethod.telex,
        .telexAdvanced,
        .vni,
    ])
    func composition(method: KeyboardInputMethod) {
        let coordinator = KeyboardInputCoordinator(inputMethod: method)
        let document = TestKeyboardWriter()
        type("t", with: coordinator, into: document)
        coordinator.handleAlternate(alternate("ế"), from: sourceKey(), writer: document)
        type("t", with: coordinator, into: document)

        #expect(document.text == "tết")
        #expect(coordinator.composer.buffer() == "tết")
    }

    @Test("English mode inserts the selected Unicode character directly")
    func english() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()
        coordinator.toggleLanguage()
        coordinator.handleAlternate(alternate("ư"), from: sourceKey(), writer: document)
        #expect(document.text == "ư")
        #expect(coordinator.composer.buffer().isEmpty)
    }

    @Test("Alternate consumes one-shot Shift")
    func oneShotShift() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()
        coordinator.handle(testKey(.shift), writer: document)
        coordinator.handleAlternate(alternate("á"), from: sourceKey(), writer: document)
        #expect(document.text == "Á")
        #expect(coordinator.state.shiftState == .lowercase)
    }

    private func alternate(_ text: String) -> KeyAlternate {
        KeyAlternate(text: text)
    }

    private func sourceKey() -> KeySpec {
        KeySpec(id: "alternate-a", label: "a", role: .character, shiftedLabel: "A")
    }
}
#endif
