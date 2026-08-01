#if os(iOS) && canImport(FunputCore)
import KeyboardInput
import KeyboardLayout
import Testing

@MainActor
struct KeyboardInputCompositionTests {
    @Test("VNI is the default and composes tone and stroke modifiers")
    func vni() {
        let coordinator = KeyboardInputCoordinator()
        let document = TestKeyboardWriter()

        type("ma", with: coordinator, into: document)
        type("1", role: .vniModifier, with: coordinator, into: document)
        coordinator.handle(testKey(.space), writer: document)
        type("d", with: coordinator, into: document)
        type("9", role: .vniModifier, with: coordinator, into: document)

        #expect(coordinator.state.inputMethod == .vni)
        #expect(document.text == "má đ")
    }

    @Test("Telex composes Vietnamese and restores ASCII words")
    func telexAndASCII() {
        let coordinator = KeyboardInputCoordinator()
        let document = TestKeyboardWriter()
        coordinator.handle(testKey(.inputMethod), writer: document)

        type("as", with: coordinator, into: document)
        coordinator.handle(testKey(.space), writer: document)
        type("card", with: coordinator, into: document)
        coordinator.handle(testKey(.space), writer: document)
        type("dd", with: coordinator, into: document)

        #expect(coordinator.state.inputMethod == .telex)
        #expect(document.text == "á card đ")
    }

    @Test("Return commits a newline and starts a fresh composition")
    func newLine() {
        let coordinator = KeyboardInputCoordinator()
        let document = TestKeyboardWriter()

        type("a", with: coordinator, into: document)
        type("1", role: .vniModifier, with: coordinator, into: document)
        coordinator.handle(testKey(.enter), writer: document)
        type("a", with: coordinator, into: document)
        type("1", role: .vniModifier, with: coordinator, into: document)

        #expect(document.text == "á\ná")
    }

    @Test("Backspace keeps the corrected composition context")
    func backspace() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()

        type("Phua", with: coordinator, into: document)
        coordinator.handle(testKey(.backspace), writer: document)
        type("s", with: coordinator, into: document)

        #expect(document.text == "Phú")
    }

    @Test("Repeated Backspace keeps composer and document synchronized")
    func repeatedBackspace() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()

        type("Phua", with: coordinator, into: document)
        coordinator.handle(testKey(.backspace), writer: document)
        coordinator.handle(testKey(.backspace), writer: document)
        type("os", with: coordinator, into: document)

        #expect(document.text == "Phó")
    }
}
#endif
