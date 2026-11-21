#if os(iOS) && canImport(FunputCore)
import KeyboardInput
import KeyboardLayout
import Testing

/// The words read back from the document after the host changed it: the bar may ask
/// after them, and the store never learns from them.
///
/// Read alongside Android's `BorrowedContextTest`: the rules are the same.
@MainActor
@Suite("Borrowed suggestion context")
struct BorrowedContextTests {
    @Test("A field that already holds text lends its last two words")
    func lendsTheLastWords() {
        let update = updateAfterHostWrote("Xin cảm ơn ")
        #expect(update.queryContext == "cảm ơn")
        #expect(update.borrowed)
        #expect(update.learnContext == nil, "a document's words are never learned from")
    }

    @Test(
        "Only a word and one space before the caret is a word to follow",
        arguments: ["", " ", "cảm", "cảm  ", "cảm.", "cảm. ", "cảm\n", "a ", "302 "]
    )
    func otherEndingsLendNothing(text: String) {
        #expect(updateAfterHostWrote(text) == .empty)
    }

    @Test("The older word needs a single space and a word of its own")
    func olderWord() {
        #expect(updateAfterHostWrote("cảm, ơn ").queryContext == "ơn")
        #expect(updateAfterHostWrote("cảm  ơn ").queryContext == "ơn")
        #expect(updateAfterHostWrote("12xyz ").queryContext == "xyz")
        let long = String(repeating: "a", count: 33)
        #expect(updateAfterHostWrote(long + " ") == .empty)
        #expect(updateAfterHostWrote(long + " ơn ").queryContext == "ơn")
    }

    @Test("A selection lends nothing")
    func selectionLendsNothing() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        document.replaceTextExternally(with: "cảm ơn ")
        document.hasSelection = true
        coordinator.synchronizeDocument(document, event: .selectionChanged)
        #expect(coordinator.takePersonalSuggestionUpdate() == .empty)
    }

    @Test("A word typed after a borrowed one is learned on its own, then is Funput's own")
    func typedAfterBorrowed() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        document.replaceTextExternally(with: "cảm ")
        coordinator.synchronizeDocument(document, event: .textChanged)
        #expect(coordinator.takePersonalSuggestionUpdate().learnContext == nil)

        type("on", with: coordinator, into: document)
        let typing = coordinator.takePersonalSuggestionUpdate()
        #expect(typing.prefix == "on")
        #expect(typing.learnContext == nil, "still typed after the document's word")

        coordinator.handle(testKey(.space, label: " "), writer: document)
        let typed = coordinator.takePersonalSuggestionUpdate()
        #expect(typed.completedToken == "on")
        #expect(typed.queryContext == "cảm on", "the borrowed word is still asked about")
        #expect(typed.learnContext == "on")
    }

    @Test("Funput's own typing stays its own")
    func authoredStaysAuthored() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        type("xin", with: coordinator, into: document)
        coordinator.handle(testKey(.space, label: " "), writer: document)
        // The host echoes the edit Funput just made; that is not a change of its own.
        coordinator.synchronizeDocument(document, event: .textChanged)

        let update = coordinator.takePersonalSuggestionUpdate()
        #expect(update.learnContext == "xin")
        #expect(!update.borrowed)
    }

    private func updateAfterHostWrote(_ text: String) -> KeyboardSuggestionInputUpdate {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        document.replaceTextExternally(with: text)
        coordinator.synchronizeDocument(document, event: .textChanged)
        return coordinator.takePersonalSuggestionUpdate()
    }
}
#endif
