#if os(iOS) && canImport(FunputCore)
import FunputShared
import KeyboardLayout
import Testing

@testable import KeyboardInput

/// A dictionary that starts out recognizing nothing, and learns what it is told to
/// keep — the part of the contract an undo exercises.
@MainActor
private final class OpenDictionary: CorrectionDictionary {
    var kept: [String] = []
    func recognizes(_ word: String) -> Bool { kept.contains(word) }
    func keep(_ word: String) { kept.append(word) }
}

@MainActor
struct KeyboardCorrectionUndoTests {
    private let dictionary = OpenDictionary()

    private func corrected() -> (KeyboardInputCoordinator, TestKeyboardWriter) {
        let coordinator = KeyboardInputCoordinator()
        coordinator.apply(FunputConfiguration(inputMethod: .telex, typoCorrection: true))
        coordinator.correctionDictionary = dictionary
        let document = TestKeyboardWriter()
        for (index, character) in "dduwowfnh ".enumerated() {
            let key = character == " "
                ? testKey(.space)
                : testKey(.character, label: String(character))
            var touch: KeyboardTouchEvidence?
            if let scalar = String(character).unicodeScalars.first, character != " " {
                touch = index == 8
                    ? KeyboardTouchEvidence(
                        typed: scalar, typedDistance: 0.35, first: .init(scalar: "g", distance: 0.15)
                    )
                    : KeyboardTouchEvidence(typed: scalar, typedDistance: 0.35)
            }
            coordinator.handle(key, touch: touch, writer: document)
        }
        #expect(document.text == "đường ")
        return (coordinator, document)
    }

    @Test("Backspace straight after a correction puts back what was typed")
    func undo() {
        let (coordinator, document) = corrected()

        coordinator.handle(testKey(.backspace), writer: document)

        #expect(document.text == "dduwowfnh ", "the space the user never touched stays")
    }

    @Test("Undoing does not re-open the word that was just rejected")
    func doesNotReopen() {
        // `reopenPreviousWord` would hand the next keystroke the corrected word back.
        let (coordinator, document) = corrected()

        coordinator.handle(testKey(.backspace), writer: document)
        coordinator.handle(testKey(.character, label: "a"), writer: document)

        #expect(document.text == "dduwowfnh a")
    }

    @Test("A keystroke after a correction spends the one-tap undo")
    func disarmed() {
        let (coordinator, document) = corrected()

        coordinator.handle(testKey(.character, label: "a"), writer: document)
        coordinator.handle(testKey(.backspace), writer: document)

        #expect(document.text == "đường ", "an ordinary Backspace, on ordinary text")
    }

    @Test("Undoing tells the dictionary to keep the word as typed")
    func keeps() {
        let (coordinator, document) = corrected()

        coordinator.handle(testKey(.backspace), writer: document)

        #expect(dictionary.kept == ["dduwowfnh"])
    }

    @Test("An undone word is not corrected again straight away")
    func suppressed() {
        let (coordinator, document) = corrected()
        coordinator.handle(testKey(.backspace), writer: document)

        for character in "dduwowfnh " {
            let key = character == " "
                ? testKey(.space)
                : testKey(.character, label: String(character))
            let scalar = String(character).unicodeScalars.first ?? "a"
            let touch = character == " "
                ? nil
                : KeyboardTouchEvidence(
                    typed: scalar, typedDistance: 0.35, first: .init(scalar: "g", distance: 0.15)
                )
            coordinator.handle(key, touch: touch, writer: document)
        }

        #expect(document.text == "dduwowfnh dduwowfnh ")
    }

    @Test("An undone word stays uncorrected after the engine's one-time reprieve")
    func keptForGood() {
        // The engine suppresses the word once. The second time is the dictionary's.
        let (coordinator, document) = corrected()
        coordinator.handle(testKey(.backspace), writer: document)

        for _ in 0..<2 {
            for character in "dduwowfnh " {
                let key = character == " "
                    ? testKey(.space)
                    : testKey(.character, label: String(character))
                let scalar = String(character).unicodeScalars.first ?? "a"
                let touch = character == " "
                    ? nil
                    : KeyboardTouchEvidence(
                        typed: scalar, typedDistance: 0.35, first: .init(scalar: "g", distance: 0.15)
                    )
                coordinator.handle(key, touch: touch, writer: document)
            }
        }

        #expect(document.text == "dduwowfnh dduwowfnh dduwowfnh ")
        #expect(coordinator.correctionDeclines.recognized == 1)
    }
}
#endif
