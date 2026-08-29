#if os(iOS) && canImport(FunputCore)
import KeyboardInput
import KeyboardLayout
import Testing

@MainActor
@Suite("Personal suggestion token tracking")
struct PersonalSuggestionTokenTests {
    @Test("Tracks authored output and completes at a boundary")
    func completedToken() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        type("ban", with: coordinator, into: document)
        #expect(coordinator.takePersonalSuggestionUpdate().prefix == "ban")
        coordinator.handle(testKey(.space, label: " "), writer: document)
        let update = coordinator.takePersonalSuggestionUpdate()
        #expect(update.prefix.isEmpty)
        #expect(update.completedToken == "ban")
    }

    @Test("Backspace edits the bounded prefix")
    func backspace() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        type("ban", with: coordinator, into: document)
        coordinator.handle(testKey(.backspace), writer: document)
        #expect(coordinator.takePersonalSuggestionUpdate().prefix == "ba")
    }

    @Test("Candidate replaces the current prefix and learns once")
    func acceptsCandidate() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        type("ban", with: coordinator, into: document)
        #expect(
            coordinator.acceptSuggestion("bạn", replacing: "ban", writer: document) != nil
        )
        #expect(document.text == "bạn ")
        let update = coordinator.takePersonalSuggestionUpdate()
        #expect(update.completedToken == "bạn")
        #expect(update.prefix.isEmpty)
    }

    @Test("External autocorrection is never learned")
    func ignoresExternalCorrection() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        type("ban", with: coordinator, into: document)
        document.replaceTextExternally(with: "bank")
        coordinator.synchronizeDocument(document, event: .textChanged)
        #expect(coordinator.takePersonalSuggestionUpdate() == .empty)
    }

    @Test("A space keeps the finished word as context, a full stop does not")
    func separatorDecidesContext() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        type("xin", with: coordinator, into: document)
        coordinator.handle(testKey(.space, label: " "), writer: document)
        #expect(coordinator.takePersonalSuggestionUpdate().context == "xin")

        type("xin", with: coordinator, into: document)
        type(".", role: .punctuation, with: coordinator, into: document)
        let ended = coordinator.takePersonalSuggestionUpdate()
        #expect(ended.completedToken == "xin", "the word is still learned")
        #expect(ended.context == nil, "but nothing follows it")
    }

    @Test("A newline ends the sentence too")
    func newlineEndsContext() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        type("xin", with: coordinator, into: document)
        type("\n", role: .punctuation, with: coordinator, into: document)
        #expect(coordinator.takePersonalSuggestionUpdate().context == nil)
    }

    @Test("The context survives while the next word is being typed")
    func contextSpansTheNextWord() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        type("xin", with: coordinator, into: document)
        coordinator.handle(testKey(.space, label: " "), writer: document)
        _ = coordinator.takePersonalSuggestionUpdate()
        type("ch", with: coordinator, into: document)
        let update = coordinator.takePersonalSuggestionUpdate()
        #expect(update.prefix == "ch")
        #expect(update.context == "xin")
    }

    @Test("Accepting a candidate makes it the context")
    func acceptedCandidateBecomesContext() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        type("ban", with: coordinator, into: document)
        #expect(
            coordinator.acceptSuggestion("bạn", replacing: "ban", writer: document) != nil
        )
        #expect(coordinator.takePersonalSuggestionUpdate().context == "bạn")
    }

    @Test("A caret that leaves the word abandons the context")
    func caretMoveClearsContext() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        type("xin", with: coordinator, into: document)
        coordinator.handle(testKey(.space, label: " "), writer: document)
        _ = coordinator.takePersonalSuggestionUpdate()
        document.replaceTextExternally(with: "một câu khác ")
        coordinator.synchronizeDocument(document, event: .textChanged)
        #expect(coordinator.takePersonalSuggestionUpdate().context == nil)
    }

    @Test("Oversized words do not expose a query prefix")
    func boundedPrefix() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        type(String(repeating: "b", count: 33), with: coordinator, into: document)
        #expect(coordinator.takePersonalSuggestionUpdate().prefix.isEmpty)
    }
}
#endif
