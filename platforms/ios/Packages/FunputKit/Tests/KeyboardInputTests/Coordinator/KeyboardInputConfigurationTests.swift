#if os(iOS) && canImport(FunputCore)
import Testing
import FunputShared
import KeyboardInput
import KeyboardLayout

@MainActor
struct KeyboardInputConfigurationTests {
    @Test("Applying configuration updates input method and language")
    func applyUpdatesState() {
        let coordinator = KeyboardInputCoordinator()
        coordinator.apply(FunputConfiguration(inputMethod: .telex, language: .english))
        #expect(coordinator.state.inputMethod == .telex)
        #expect(coordinator.state.language == .english)
    }

    @Test("Telex configuration composes Vietnamese")
    func telexComposes() {
        let coordinator = KeyboardInputCoordinator()
        coordinator.apply(FunputConfiguration(inputMethod: .telex))
        let document = TestKeyboardWriter()
        type("as", with: coordinator, into: document)
        #expect(document.text == "á")
    }

    @Test("VNI configuration composes tone modifiers")
    func vniComposes() {
        let coordinator = KeyboardInputCoordinator()
        coordinator.apply(FunputConfiguration(inputMethod: .vni))
        let document = TestKeyboardWriter()
        type("ma", with: coordinator, into: document)
        type("1", role: .vniModifier, with: coordinator, into: document)
        #expect(document.text == "má")
    }

    @Test("English configuration disables Vietnamese composition")
    func englishPassthrough() {
        let coordinator = KeyboardInputCoordinator()
        coordinator.apply(FunputConfiguration(inputMethod: .telex, language: .english))
        let document = TestKeyboardWriter()
        type("as", with: coordinator, into: document)
        #expect(document.text == "as")
    }

    @Test("Default configuration keeps traditional tone placement")
    func defaultUsesTraditionalTone() {
        let coordinator = KeyboardInputCoordinator()
        coordinator.apply(.default)
        let document = TestKeyboardWriter()
        type("hoa", with: coordinator, into: document)
        type("2", role: .vniModifier, with: coordinator, into: document)
        #expect(document.text == "hòa") // traditional; the modern style would yield "hoà"
    }
}
#endif
