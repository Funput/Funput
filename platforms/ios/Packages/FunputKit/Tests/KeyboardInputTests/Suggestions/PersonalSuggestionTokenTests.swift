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

    @Test("Oversized words do not expose a query prefix")
    func boundedPrefix() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        type(String(repeating: "b", count: 33), with: coordinator, into: document)
        #expect(coordinator.takePersonalSuggestionUpdate().prefix.isEmpty)
    }
}
#endif
