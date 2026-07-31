#if os(iOS) && canImport(FunputCore)
import Foundation
import KeyboardInput
import Testing

/// A host acknowledges an insertion through both `textDidChange` and `selectionDidChange`.
/// While typing fast those echoes land after the next keystroke, so a selection callback
/// carrying a stale caret context must not be mistaken for an external edit.
@MainActor
struct KeyboardDocumentSelectionEchoTests {
    @Test("A late selection echo keeps the composition alive")
    func delayedSelectionEcho() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()
        document.delaysContextUpdates = true

        type("toan", with: coordinator, into: document)
        coordinator.synchronizeDocument(document, event: .selectionChanged)
        type("s", with: coordinator, into: document)

        #expect(document.text == "toán")
    }

    @Test("An active selection still clears the composition")
    func activeSelectionResets() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()

        type("toan", with: coordinator, into: document)
        document.hasSelection = true
        coordinator.synchronizeDocument(document, event: .selectionChanged)
        document.hasSelection = false
        type("s", with: coordinator, into: document)

        #expect(document.text == "toans")
    }

    @Test("An external edit reported as a selection change still clears the composition")
    func externalSelectionChangeResets() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()

        type("toan", with: coordinator, into: document)
        document.replaceTextExternally(with: "🙂toan")
        coordinator.synchronizeDocument(document, event: .selectionChanged)
        type("s", with: coordinator, into: document)

        #expect(document.text == "🙂toans")
    }
}
#endif
