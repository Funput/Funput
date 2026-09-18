#if os(iOS) && canImport(FunputCore)
import Foundation
import KeyboardInput
import KeyboardLayout
import Testing

@MainActor
struct KeyboardAutocapitalizationDeletionTests {
    @Test("Deleting a sentence boundary disarms automatic Shift")
    func deletionRecomputesAutomaticShift() {
        let (coordinator, document) = makeSubject(context: "Hello")

        type(".", role: .punctuation, with: coordinator, into: document)
        type(" ", role: .space, with: coordinator, into: document)
        #expect(coordinator.state.shiftState == .uppercase)

        coordinator.handle(testKey(.backspace), writer: document)
        #expect(document.text == "Hello.")
        #expect(coordinator.state.shiftState == .lowercase)

        coordinator.handle(testKey(.backspace), writer: document)
        #expect(document.text == "Hello")
        #expect(coordinator.state.shiftState == .lowercase)
    }

    @Test("Backspace preserves Shift when the user enabled it")
    func deletionPreservesManualShift() {
        let (coordinator, document) = makeSubject(context: "Hello.")

        coordinator.handle(testKey(.shift), writer: document)
        coordinator.handle(testKey(.backspace), writer: document)

        #expect(document.text == "Hello")
        #expect(coordinator.state.shiftState == .uppercase)
    }

    private func makeSubject(
        context: String
    ) -> (KeyboardInputCoordinator, TestKeyboardWriter) {
        let coordinator = KeyboardInputCoordinator()
        let document = TestKeyboardWriter()
        document.replaceTextExternally(with: context)
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
