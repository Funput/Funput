#if os(iOS) && canImport(FunputCore)
import KeyboardInput
import KeyboardLayout
import Testing

@MainActor
struct KeyboardInputStateTests {
    @Test("Switching methods clears the active composition")
    func methodSwitchClearsComposition() {
        let coordinator = KeyboardInputCoordinator()
        let document = TestKeyboardDocument()

        type("a", with: coordinator, into: document)
        coordinator.handle(testKey(.inputMethod), document: document)
        type("s", with: coordinator, into: document)

        #expect(coordinator.state.inputMethod == .telex)
        #expect(document.text == "as")
    }

    @Test("Shift uppercases one character and then resets")
    func oneShotShift() {
        let coordinator = KeyboardInputCoordinator()
        let document = TestKeyboardDocument()

        coordinator.handle(testKey(.shift), document: document)
        type("x", with: coordinator, into: document)
        type("i", with: coordinator, into: document)

        #expect(document.text == "Xi")
        #expect(coordinator.state.shiftState == .lowercase)
    }

    @Test("Non-character keys do not consume one-shot Shift")
    func modifierKeepsShift() {
        let coordinator = KeyboardInputCoordinator()
        let document = TestKeyboardDocument()

        coordinator.handle(testKey(.shift), document: document)
        type("1", role: .vniModifier, with: coordinator, into: document)

        #expect(coordinator.state.shiftState == .uppercase)
    }
}
#endif
