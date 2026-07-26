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
        let document = AlternateTestDocument()
        type("t", coordinator: coordinator, document: document)
        coordinator.handleAlternate(
            KeyAlternate(text: "ế"),
            from: KeySpec(id: "e", label: "e", role: .character),
            document: document
        )
        type("t", coordinator: coordinator, document: document)
        #expect(document.text == "tết")
    }

    @Test("English mode inserts Unicode and one-shot Shift is consumed")
    func languageAndShift() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = AlternateTestDocument()
        coordinator.toggleLanguage()
        coordinator.handle(
            KeySpec(id: "shift", label: "", role: .shift),
            document: document
        )
        coordinator.handleAlternate(
            KeyAlternate(text: "ư"),
            from: KeySpec(id: "u", label: "u", role: .character),
            document: document
        )
        #expect(document.text == "Ư")
        #expect(coordinator.state.shiftState == .lowercase)
    }

    private func type(
        _ text: String,
        coordinator: KeyboardInputCoordinator,
        document: AlternateTestDocument
    ) {
        for character in text {
            coordinator.handle(
                KeySpec(
                    id: "key-\(character)",
                    label: String(character),
                    role: .character
                ),
                document: document
            )
        }
    }
}

@MainActor
private final class AlternateTestDocument: KeyboardDocument {
    var text = ""
    let documentIdentifier = UUID()
    var contextBeforeInput: String? { text }
    let hasSelection = false

    func insertText(_ text: String) {
        self.text.append(text)
    }

    func deleteBackward() {
        if !text.isEmpty { text.removeLast() }
    }
}
