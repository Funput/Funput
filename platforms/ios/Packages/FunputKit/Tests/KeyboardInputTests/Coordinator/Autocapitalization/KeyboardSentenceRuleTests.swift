#if os(iOS) && canImport(FunputCore)
import Foundation
import KeyboardInput
import KeyboardLayout
import Testing

/// The two rules iOS gained by reading sentence boundaries from the shared core
/// rules rather than from a Swift copy of them. Asserted through the coordinator,
/// so they cover the whole path a keystroke takes; the rules themselves are pinned
/// in FunputSentenceTests and in funput-core.
@MainActor
struct KeyboardSentenceRuleTests {
    /// The quote sits between the full stop and the space, and is read past rather
    /// than taken as the character before the caret.
    @Test("A closing quote does not hide the sentence end behind it")
    func characterAfterAClosingQuote() {
        let (coordinator, document) = makeSubject(context: "Anh ay noi \"Xin chao.\" ")

        type("r", with: coordinator, into: document)

        #expect(document.text == "Anh ay noi \"Xin chao.\" R")
    }

    /// A keyboard commits its capital before the user can see it, so it takes the
    /// abbreviation guard that the bulk case transform declines.
    @Test("A character after an abbreviation stays lower case")
    func characterAfterAnAbbreviation() {
        let (coordinator, document) = makeSubject(context: "giay to v.v. ")

        type("n", with: coordinator, into: document)

        #expect(document.text == "giay to v.v. n")
    }

    /// The guard needs an earlier full stop to recognise an abbreviation, so a single
    /// dotted word still reads as an ending.
    @Test("A single dotted word still ends a sentence")
    func characterAfterASingleDottedWord() {
        let (coordinator, document) = makeSubject(context: "TS. ")

        type("n", with: coordinator, into: document)

        #expect(document.text == "TS. N")
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
