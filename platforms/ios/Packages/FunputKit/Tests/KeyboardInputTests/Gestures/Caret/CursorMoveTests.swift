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

        coordinator.moveCaret(columns: -3, lines: 0, writer: document)

        #expect(document.transactions.count == 1)
        #expect(document.transactions[0].mutations == [.moveCursor(offset: -3)])
        #expect(document.text == "xin chao")
        #expect(document.caret == 5)
    }

    @Test("A zero move writes nothing")
    func zeroIsNoOp() {
        let (coordinator, document) = makeCoordinator(context: "xin chao")

        let effects = coordinator.moveCaret(columns: 0, lines: 0, writer: document)

        #expect(effects == .none)
        #expect(document.transactions.isEmpty)
    }

    @Test("Moving the caret drops the document shadow")
    func invalidatesShadow() {
        let (coordinator, document) = makeCoordinator(context: "xin chao")

        coordinator.moveCaret(columns: -2, lines: 0, writer: document)

        #expect(coordinator.documentSynchronizer.snapshot == nil)
    }

    @Test("Typing after a move composes at the new caret, not the old one")
    func typesAtNewCaret() {
        let (coordinator, document) = makeCoordinator(context: "xin chao")

        coordinator.moveCaret(columns: -4, lines: 0, writer: document)
        coordinator.synchronizeDocument(document, event: .selectionChanged)
        type("Z", with: coordinator, into: document)

        #expect(document.text == "xin Zchao")
    }

    @Test("A vertical step lands on the same column of the line above")
    func movesUpToTheSameColumn() {
        let (coordinator, document) = makeCoordinator(context: "abcdefgh\nij\nklmnop")

        coordinator.moveCaret(columns: 0, lines: -1, writer: document)

        // Column 6 does not fit on "ij", so the caret stops at its end.
        #expect(document.caret == 11)
    }

    @Test("A second vertical step returns to the column the first one wanted")
    func remembersTheColumnAcrossSteps() {
        let (coordinator, document) = makeCoordinator(context: "abcdefgh\nij\nklmnop")

        coordinator.moveCaret(columns: 0, lines: -1, writer: document)
        coordinator.moveCaret(columns: 0, lines: -1, writer: document)

        #expect(document.caret == 6)
    }

    @Test("A horizontal step in between forgets the remembered column")
    func horizontalStepForgetsTheColumn() {
        let (coordinator, document) = makeCoordinator(context: "abcdefgh\nij\nklmnop")

        coordinator.moveCaret(columns: 0, lines: -1, writer: document)
        coordinator.moveCaret(columns: 1, lines: 0, writer: document)
        coordinator.moveCaret(columns: 0, lines: -1, writer: document)

        // Column 0 of "ij", not column 6 of the line the pan started on.
        #expect(document.caret == 9)
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
