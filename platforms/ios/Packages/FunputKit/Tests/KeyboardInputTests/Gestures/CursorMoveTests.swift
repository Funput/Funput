#if os(iOS) && canImport(FunputCore)
import Foundation
@testable import KeyboardInput
import KeyboardLayout
import Testing

@MainActor
struct CursorMoveTests {
    @Test("A move produces one caret mutation and no text change")
    func emitsMoveMutation() {
        let (coordinator, document) = makeCoordinator(context: "xin chao")

        coordinator.moveCursor(by: -3, writer: document)

        #expect(document.transactions.count == 1)
        #expect(document.transactions[0].mutations == [.moveCursor(offset: -3)])
        #expect(document.text == "xin chao")
        #expect(document.caret == 5)
    }

    @Test("A zero move writes nothing")
    func zeroIsNoOp() {
        let (coordinator, document) = makeCoordinator(context: "xin chao")

        let effects = coordinator.moveCursor(by: 0, writer: document)

        #expect(effects == .none)
        #expect(document.transactions.isEmpty)
    }

    @Test("Moving the caret drops the document shadow")
    func invalidatesShadow() {
        let (coordinator, document) = makeCoordinator(context: "xin chao")

        coordinator.moveCursor(by: -2, writer: document)

        #expect(coordinator.documentSynchronizer.snapshot == nil)
    }

    @Test("Typing after a move composes at the new caret, not the old one")
    func typesAtNewCaret() {
        let (coordinator, document) = makeCoordinator(context: "xin chao")

        coordinator.moveCursor(by: -4, writer: document)
        coordinator.synchronizeDocument(document, event: .selectionChanged)
        type("Z", with: coordinator, into: document)

        #expect(document.text == "xin Zchao")
    }

    private func makeCoordinator(
        context: String
    ) -> (KeyboardInputCoordinator, TestKeyboardWriter) {
        let coordinator = KeyboardInputCoordinator()
        let document = TestKeyboardWriter()
        document.replaceTextExternally(with: context)
        coordinator.updateContext(inputContext(
            editorMode: .text,
            enterAction: .newLine
        ))
        coordinator.synchronizeDocument(document, event: .activated)
        return (coordinator, document)
    }
}
#endif
