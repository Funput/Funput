#if os(iOS) && canImport(FunputCore)
import Foundation
@testable import KeyboardInput
import KeyboardLayout
import Testing

@MainActor
struct WordDeleteTests {
    @Test(
        "The span behind the caret covers trailing spaces and the whole word",
        arguments: [
            ("xin chào", 4),
            ("xin chào ", 5),
            ("xin   ", 6),
            ("a!!!", 3),
        ]
    )
    func span(context: String, expected: Int) {
        #expect(KeyboardWordDeletion.spanBeforeCursor(context) == expected)
    }

    @Test("An empty or unknown context has no measurable span")
    func unmeasurable() {
        #expect(KeyboardWordDeletion.spanBeforeCursor("") == nil)
        #expect(KeyboardWordDeletion.spanBeforeCursor(nil) == nil)
    }

    @Test("Deleting a word removes it together with the space before the caret")
    func deletesWordAndSpace() {
        let (coordinator, document) = makeCoordinator(context: "xin chào ")

        coordinator.deleteWordBackward(writer: document)

        #expect(document.text == "xin ")
    }

    @Test("A Vietnamese word with diacritics is removed whole")
    func deletesDiacritics() {
        let (coordinator, document) = makeCoordinator(context: "xin chào")

        coordinator.deleteWordBackward(writer: document)

        #expect(document.text == "xin ")
    }

    @Test("Without context the gesture degrades to one backspace")
    func unknownContextDeletesOne() {
        let (coordinator, document) = makeCoordinator(context: "xin chao")
        document.exposesContext = false
        coordinator.synchronizeDocument(document, event: .activated)

        coordinator.deleteWordBackward(writer: document)

        #expect(document.text == "xin cha")
    }

    @Test("Deleting a word does not reopen it for retoning")
    func doesNotReopenWord() {
        let (coordinator, document) = makeCoordinator(context: "xin chào")

        coordinator.deleteWordBackward(writer: document)
        type("s", with: coordinator, into: document)

        #expect(document.text == "xin s")
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
