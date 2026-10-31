#if os(iOS) && canImport(FunputCore)
import KeyboardInput
import KeyboardLayout
import Testing

/// The two words a prefix is typed after, which is what the engine's trigram reads:
/// kept while the words were typed in a row, dropped at every boundary that drops one.
///
/// Read alongside Android's `AuthoredTokenTwoWordContextTest`: the rules are the same.
@MainActor
@Suite("Two words of suggestion context")
struct PersonalSuggestionTwoWordContextTests {
    @Test("Two words typed in a row are both the context, oldest first")
    func twoWordsInARow() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        words(["xin", "chao"], with: coordinator, into: document)
        let update = coordinator.takePersonalSuggestionUpdate()
        #expect(update.context == "chao")
        #expect(update.older == "xin")
        #expect(update.queryContext == "xin chao")
    }

    @Test("Only the last two words are kept")
    func lastTwoOnly() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        words(["xin", "chao", "ban"], with: coordinator, into: document)
        #expect(coordinator.takePersonalSuggestionUpdate().queryContext == "chao ban")
    }

    @Test("One word alone is a one word context")
    func oneWord() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        words(["chao"], with: coordinator, into: document)
        let update = coordinator.takePersonalSuggestionUpdate()
        #expect(update.older == nil)
        #expect(update.queryContext == "chao")
    }

    @Test("A full stop between the words keeps only the second")
    func fullStopBetween() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        type("xin", with: coordinator, into: document)
        type(".", role: .punctuation, with: coordinator, into: document)
        // The keyboard takes an update after every key, as the service does.
        _ = coordinator.takePersonalSuggestionUpdate()
        coordinator.handle(testKey(.space, label: " "), writer: document)
        _ = coordinator.takePersonalSuggestionUpdate()
        words(["chao"], with: coordinator, into: document)
        let update = coordinator.takePersonalSuggestionUpdate()
        #expect(update.older == nil, "the words either side of the stop were never adjacent")
        #expect(update.queryContext == "chao")
    }

    @Test("An accepted prediction follows the word before it")
    func acceptedPrediction() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        words(["xin"], with: coordinator, into: document)
        _ = coordinator.takePersonalSuggestionUpdate()
        #expect(coordinator.acceptSuggestion("chào", replacing: "", writer: document) != nil)
        #expect(coordinator.takePersonalSuggestionUpdate().queryContext == "xin chào")
    }

    @Test("Backspacing past a boundary and a caret move drop both words")
    func boundariesDropBoth() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        words(["xin", "chao"], with: coordinator, into: document)
        coordinator.handle(testKey(.backspace), writer: document)
        let crossed = coordinator.takePersonalSuggestionUpdate()
        #expect(crossed.context == nil)
        #expect(crossed.older == nil)

        words(["xin", "chao"], with: coordinator, into: document)
        _ = coordinator.takePersonalSuggestionUpdate()
        document.replaceTextExternally(with: "một câu khác ")
        coordinator.synchronizeDocument(document, event: .textChanged)
        #expect(coordinator.takePersonalSuggestionUpdate().queryContext == nil)
    }

    @Test("An update with no context carries no older word either")
    func olderNeedsContext() {
        let update = KeyboardSuggestionInputUpdate(prefix: "", completedToken: nil, context: nil, older: "xin")
        #expect(update.older == nil)
        #expect(update.queryContext == nil)
    }

    private func words(
        _ words: [String],
        with coordinator: KeyboardInputCoordinator,
        into document: TestKeyboardWriter
    ) {
        for word in words {
            type(word, with: coordinator, into: document)
            coordinator.handle(testKey(.space, label: " "), writer: document)
        }
    }
}
#endif
