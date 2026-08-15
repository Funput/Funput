#if os(iOS) && canImport(FunputCore)
import FunputShared
import KeyboardInput
import KeyboardLayout
import Testing

/// On iOS the Shift state is the only thing that decides a letter's case: the keyboard
/// draws its own keys, so the user can see and change it.
///
/// The engine keeps a separate sentence tracker for the desktop shells, and it receives
/// the same preference, so these tests pin down that it never gets the last word — the
/// output follows Shift even when the user lowers it right where the engine's tracker
/// would capitalize. Delete them and a change in the composition pipeline could start
/// overriding the user silently.
@MainActor
struct KeyboardCapitalizationOwnershipTests {
    @Test("Turning Shift off after a sentence end is respected")
    func manualShiftOffSurvivesSentenceBoundary() {
        let (coordinator, document) = makeCoordinator()

        type("ok", with: coordinator, into: document)
        type(".", role: .punctuation, with: coordinator, into: document)
        type(" ", role: .space, with: coordinator, into: document)
        #expect(coordinator.state.shiftState == .uppercase)

        coordinator.handle(testKey(.shift), writer: document)
        #expect(coordinator.state.shiftState == .lowercase)
        type("l", with: coordinator, into: document)

        #expect(document.text == "Ok. l")
    }

    @Test("The Shift state still capitalizes after a sentence end on its own")
    func shiftStateStillArms() {
        let (coordinator, document) = makeCoordinator()

        type("ok", with: coordinator, into: document)
        type(".", role: .punctuation, with: coordinator, into: document)
        type(" ", role: .space, with: coordinator, into: document)
        type("l", with: coordinator, into: document)

        #expect(document.text == "Ok. L")
    }

    private func makeCoordinator() -> (KeyboardInputCoordinator, TestKeyboardWriter) {
        let coordinator = KeyboardInputCoordinator()
        let document = TestKeyboardWriter()
        var value = FunputConfiguration.default
        value.autoCapitalize = true
        coordinator.apply(value)
        coordinator.updateContext(inputContext(
            editorMode: .text,
            enterAction: .newLine,
            autocapitalization: .sentences
        ))
        coordinator.synchronizeDocument(document, event: .activated)
        return (coordinator, document)
    }
}
#endif
