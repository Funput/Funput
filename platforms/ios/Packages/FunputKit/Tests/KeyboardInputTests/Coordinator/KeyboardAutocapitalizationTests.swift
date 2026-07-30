#if os(iOS) && canImport(FunputCore)
import Foundation
import KeyboardInput
import KeyboardLayout
import Testing

@MainActor
struct KeyboardAutocapitalizationTests {
    @Test("Capitalization modes resolve from document context")
    func modes() {
        assertShift(.lowercase, mode: .none, context: "")
        assertShift(.uppercase, mode: .allCharacters, context: "text")
        assertShift(.uppercase, mode: .words, context: "hello ")
        assertShift(.lowercase, mode: .words, context: "hello")
        assertShift(.uppercase, mode: .sentences, context: "Hello. ")
        assertShift(.uppercase, mode: .sentences, context: "Hello\n")
        assertShift(.lowercase, mode: .sentences, context: "Hello")
    }

    @Test("Nil context preserves the current Shift state")
    func unavailableContext() {
        let (coordinator, document) = makeCoordinator(mode: .sentences, context: "text")
        coordinator.handle(testKey(.shift), writer: document)
        document.exposesContext = false

        coordinator.synchronizeDocument(document, event: .textChanged)

        #expect(coordinator.state.shiftState == .uppercase)
    }

    @Test("All-characters mode rearms Shift after every character")
    func allCharacters() {
        let (coordinator, document) = makeCoordinator(mode: .allCharacters, context: "")

        type("ab", with: coordinator, into: document)

        #expect(document.text == "AB")
        #expect(coordinator.state.shiftState == .uppercase)
    }

    @Test("Caps Lock survives edits but resets for a new document")
    func capsLockLifetime() {
        var time = 1.0
        let coordinator = KeyboardInputCoordinator(shiftClock: { time })
        let document = TestKeyboardWriter()
        document.replaceTextExternally(with: "hello")
        coordinator.updateContext(inputContext(
            editorMode: .text,
            enterAction: .newLine,
            autocapitalization: .sentences
        ))
        coordinator.synchronizeDocument(document, event: .activated)
        coordinator.handle(testKey(.shift), writer: document)
        time += 0.1
        coordinator.handle(testKey(.shift), writer: document)

        document.replaceTextExternally(with: "hello ")
        coordinator.synchronizeDocument(document, event: .textChanged)
        #expect(coordinator.state.shiftState == .capsLocked)

        document.documentIdentifier = UUID()
        coordinator.synchronizeDocument(document, event: .textChanged)
        #expect(coordinator.state.shiftState == .lowercase)
    }

    private func assertShift(
        _ expected: ShiftState,
        mode: KeyboardAutocapitalizationMode,
        context: String
    ) {
        let (coordinator, _) = makeCoordinator(mode: mode, context: context)
        #expect(coordinator.state.shiftState == expected)
    }

    private func makeCoordinator(
        mode: KeyboardAutocapitalizationMode,
        context: String
    ) -> (KeyboardInputCoordinator, TestKeyboardWriter) {
        let coordinator = KeyboardInputCoordinator()
        let document = TestKeyboardWriter()
        document.replaceTextExternally(with: context)
        coordinator.updateContext(inputContext(
            editorMode: .text,
            enterAction: .newLine,
            autocapitalization: mode
        ))
        coordinator.synchronizeDocument(document, event: .activated)
        return (coordinator, document)
    }
}
#endif
