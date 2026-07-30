import Foundation
import KeyboardInput
import KeyboardLayout
import Testing

@MainActor
struct KeyboardAlternateInputIntegrationTests {
    @Test("Precomposed alternate continues a word", arguments: [
        KeyboardInputMethod.telex,
        .telexAdvanced,
        .vni,
    ])
    func composition(method: KeyboardInputMethod) {
        let coordinator = KeyboardInputCoordinator(inputMethod: method)
        let document = ScriptedWriter()
        type("t", coordinator: coordinator, writer: document)
        coordinator.handleAlternate(
            KeyAlternate(text: "ế"),
            from: KeySpec(id: "e", label: "e", role: .character),
            writer: document
        )
        type("t", coordinator: coordinator, writer: document)
        #expect(document.text == "tết")
    }

    @Test("English mode inserts Unicode and one-shot Shift is consumed")
    func languageAndShift() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = ScriptedWriter()
        coordinator.toggleLanguage()
        coordinator.handle(
            KeySpec(id: "shift", label: "", role: .shift),
            writer: document
        )
        coordinator.handleAlternate(
            KeyAlternate(text: "ư"),
            from: KeySpec(id: "u", label: "u", role: .character),
            writer: document
        )
        #expect(document.text == "Ư")
        #expect(coordinator.state.shiftState == .lowercase)
    }

    private func type(
        _ text: String,
        coordinator: KeyboardInputCoordinator,
        writer: ScriptedWriter
    ) {
        for character in text {
            coordinator.handle(
                KeySpec(
                    id: "key-\(character)",
                    label: String(character),
                    role: .character
                ),
                writer: writer
            )
        }
    }
}
