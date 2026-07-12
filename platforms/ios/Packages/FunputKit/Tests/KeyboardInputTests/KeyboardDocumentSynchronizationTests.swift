#if os(iOS) && canImport(FunputCore)
import Foundation
import KeyboardInput
import Testing

@MainActor
struct KeyboardDocumentSynchronizationTests {
    @Test("Coordinator-authored mutations remain synchronized")
    func ownMutations() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardDocument()

        type("a", with: coordinator, into: document)
        coordinator.synchronizeDocument(document, event: .textChanged)
        type("s", with: coordinator, into: document)

        #expect(document.text == "á")
    }

    @Test("External text and caret context changes clear composition")
    func externalContextChange() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardDocument()
        type("a", with: coordinator, into: document)

        document.replaceTextExternally(with: "🙂a")
        coordinator.synchronizeDocument(document, event: .selectionChanged)
        type("s", with: coordinator, into: document)

        #expect(document.text == "🙂as")
    }

    @Test("A new document resets composition even with identical context")
    func documentChange() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardDocument()
        type("a", with: coordinator, into: document)

        document.documentIdentifier = UUID()
        coordinator.synchronizeDocument(document, event: .textChanged)
        type("s", with: coordinator, into: document)

        #expect(document.text == "as")
    }

    @Test("An active selection clears composition")
    func selection() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardDocument()
        type("a", with: coordinator, into: document)

        document.hasSelection = true
        coordinator.synchronizeDocument(document, event: .selectionChanged)
        document.hasSelection = false
        type("s", with: coordinator, into: document)

        #expect(document.text == "as")
    }

    @Test("Unavailable context does not disable Vietnamese composition")
    func unavailableContext() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardDocument()
        document.exposesContext = false

        type("as", with: coordinator, into: document)

        #expect(document.text == "á")
    }
}
#endif
