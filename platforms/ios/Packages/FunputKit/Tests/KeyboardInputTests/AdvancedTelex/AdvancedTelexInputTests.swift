#if os(iOS) && canImport(FunputCore)
import FunputShared
import KeyboardInput
import KeyboardLayout
import Testing

@MainActor
struct AdvancedTelexInputTests {
    @Test("Configuration applies Advanced Telex composition")
    func configuration() {
        let coordinator = KeyboardInputCoordinator()
        coordinator.apply(FunputConfiguration(inputMethod: .telexAdvanced))
        let document = TestKeyboardWriter()

        typeAdvanced("tr[]ngf", with: coordinator, into: document)

        #expect(coordinator.state.inputMethod == .telexAdvanced)
        #expect(document.text == "trường")
    }

    @Test("Runtime toggle returns to the preferred Telex variant")
    func preferredToggle() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telexAdvanced)
        let document = TestKeyboardWriter()

        coordinator.handle(testKey(.inputMethod), writer: document)
        #expect(coordinator.state.inputMethod == .vni)
        coordinator.handle(testKey(.inputMethod), writer: document)
        #expect(coordinator.state.inputMethod == .telexAdvanced)
    }

    @Test("Brackets remain inside the delayed synchronization epoch")
    func delayedBracketEchoes() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telexAdvanced)
        let document = TestKeyboardWriter()
        document.delaysContextUpdates = true
        document.publishContext()

        type("tr", with: coordinator, into: document)
        type("[", role: .punctuation, with: coordinator, into: document)
        type("]", role: .punctuation, with: coordinator, into: document)
        coordinator.synchronizeDocument(document, event: .textChanged)
        type("ngf", with: coordinator, into: document)

        #expect(document.text == "trường")
    }

    @Test("Standard Telex and VNI keep brackets literal")
    func bracketsStayLiteral() {
        for method in [KeyboardInputMethod.telex, .vni] {
            let coordinator = KeyboardInputCoordinator(inputMethod: method)
            let document = TestKeyboardWriter()
            type("[", role: .punctuation, with: coordinator, into: document)
            type("]", role: .punctuation, with: coordinator, into: document)
            #expect(document.text == "[]")
        }
    }

    private func typeAdvanced(
        _ keys: String,
        with coordinator: KeyboardInputCoordinator,
        into document: TestKeyboardWriter
    ) {
        for character in keys {
            let role: KeyRole = character == "[" || character == "]"
                ? .punctuation
                : .character
            type(String(character), role: role, with: coordinator, into: document)
        }
    }
}
#endif
