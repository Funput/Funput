#if os(iOS) && canImport(FunputCore)
import KeyboardInput
import KeyboardLayout
import Testing

/// Re-opening a committed word after Backspace, so the next keystroke retones it.
///
/// The document is never rewritten here — seeding the composer with text that is
/// already at the caret is enough, because the buffer mirrors committed text.
@MainActor
struct ReopenPreviousWordTests {
    private func typedWord() -> (KeyboardInputCoordinator, TestKeyboardWriter) {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()
        type("chaof ", with: coordinator, into: document) // commits "chào " on the space
        #expect(document.text == "chào ")
        return (coordinator, document)
    }

    @Test("Backspace over the space re-opens the word for a new tone")
    func retonesAfterBackspace() {
        let (coordinator, document) = typedWord()

        coordinator.handle(testKey(.backspace), writer: document)
        type("s", with: coordinator, into: document)

        #expect(document.text == "cháo")
    }

    /// The proxy can report text from before the edit. Reading the synchronizer's
    /// shadow context instead makes this path immune — the same class of failure that
    /// broke the first Android attempt.
    @Test("Works even when the document reports stale context")
    func survivesStaleContext() {
        let (coordinator, document) = typedWord()
        document.delaysContextUpdates = true

        coordinator.handle(testKey(.backspace), writer: document)
        type("s", with: coordinator, into: document)

        #expect(document.text == "cháo")
    }

    @Test("A word that is not a syllable stays committed")
    func leavesNonSyllableAlone() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()
        type("hello ", with: coordinator, into: document)

        coordinator.handle(testKey(.backspace), writer: document)
        type("s", with: coordinator, into: document)

        // `s` is typed literally: the English word was never re-opened.
        #expect(document.text == "hellos")
    }

    @Test("A caret behind punctuation re-opens nothing")
    func skipsAfterPunctuation() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()
        type("chaof, ", with: coordinator, into: document)
        #expect(document.text == "chào, ")

        coordinator.handle(testKey(.backspace), writer: document) // deletes the space
        type("s", with: coordinator, into: document)

        #expect(document.text == "chào,s")
    }

    @Test("Backspace inside a live composition still edits it, not the word before")
    func doesNotDisturbLiveComposition() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()
        type("chaof meo", with: coordinator, into: document)

        coordinator.handle(testKey(.backspace), writer: document) // drops the "o"
        type("os", with: coordinator, into: document)

        // The live word kept composing: "me" + o + s tones the nucleus.
        #expect(document.text == "chào méo")
    }

    @Test("A selection is left untouched")
    func skipsWithSelection() {
        let (coordinator, document) = typedWord()
        document.hasSelection = true
        // The shadow learns about a selection from the host's lifecycle callback,
        // the way `KeyboardViewController+Traits` reports it.
        coordinator.synchronizeDocument(document, event: .selectionChanged)

        coordinator.handle(testKey(.backspace), writer: document)
        type("s", with: coordinator, into: document)

        // Nothing was re-opened, so "s" is a literal letter.
        #expect(document.text == "chàos")
    }
}
#endif
