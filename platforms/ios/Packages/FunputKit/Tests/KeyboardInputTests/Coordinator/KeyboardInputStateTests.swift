#if os(iOS) && canImport(FunputCore)
import KeyboardInput
import KeyboardLayout
import Testing

@MainActor
struct KeyboardInputStateTests {
    @Test("Switching methods clears the active composition")
    func methodSwitchClearsComposition() {
        let coordinator = KeyboardInputCoordinator()
        let document = TestKeyboardWriter()

        type("a", with: coordinator, into: document)
        coordinator.handle(testKey(.inputMethod), writer: document)
        type("s", with: coordinator, into: document)

        #expect(coordinator.state.inputMethod == .telex)
        #expect(document.text == "as")
    }

    @Test("Shift uppercases one character and then resets")
    func oneShotShift() {
        let coordinator = KeyboardInputCoordinator()
        let document = TestKeyboardWriter()

        coordinator.handle(testKey(.shift), writer: document)
        type("x", with: coordinator, into: document)
        type("i", with: coordinator, into: document)

        #expect(document.text == "Xi")
        #expect(coordinator.state.shiftState == .lowercase)
    }

    @Test("Non-character keys do not consume one-shot Shift")
    func modifierKeepsShift() {
        let coordinator = KeyboardInputCoordinator()
        let document = TestKeyboardWriter()

        coordinator.handle(testKey(.shift), writer: document)
        type("1", role: .vniModifier, with: coordinator, into: document)

        #expect(coordinator.state.shiftState == .uppercase)
    }

    @Test("Double-tapping Shift from lowercase enables Caps Lock until Shift is tapped again")
    func capsLockFromLowercase() {
        var time = 10.0
        let coordinator = KeyboardInputCoordinator(shiftClock: { time })
        let document = TestKeyboardWriter()

        coordinator.handle(testKey(.shift), writer: document)
        time += 0.1
        coordinator.handle(testKey(.shift), writer: document)
        type("xi", with: coordinator, into: document)

        #expect(document.text == "XI")
        #expect(coordinator.state.shiftState == .capsLocked)

        coordinator.handle(testKey(.shift), writer: document)
        #expect(coordinator.state.shiftState == .lowercase)
    }

    @Test("Double-tapping Shift from uppercase enables Caps Lock")
    func capsLockFromUppercase() {
        var time = 10.0
        let coordinator = KeyboardInputCoordinator(shiftClock: { time })
        let document = TestKeyboardWriter()
        coordinator.updateContext(inputContext(
            editorMode: .text,
            enterAction: .newLine,
            autocapitalization: .sentences
        ))
        coordinator.synchronizeDocument(document, event: .activated)
        #expect(coordinator.state.shiftState == .uppercase)

        coordinator.handle(testKey(.shift), writer: document)
        time += 0.1
        coordinator.handle(testKey(.shift), writer: document)
        type("xi", with: coordinator, into: document)

        #expect(document.text == "XI")
        #expect(coordinator.state.shiftState == .capsLocked)
    }

    @Test("Layout navigation switches between letters and symbol pages")
    func layoutNavigation() {
        let coordinator = KeyboardInputCoordinator()
        let document = TestKeyboardWriter()

        coordinator.handle(testKey(.symbols), writer: document)
        #expect(coordinator.state.layoutMode == .symbolsPrimary)
        coordinator.handle(testKey(.moreSymbols), writer: document)
        #expect(coordinator.state.layoutMode == .symbolsSecondary)
        coordinator.handle(testKey(.letters), writer: document)
        #expect(coordinator.state.layoutMode == .letters)
    }
}
#endif
