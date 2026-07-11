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

    @Test("Double-tapping Shift enables Caps Lock until Shift is tapped again")
    func capsLock() {
        var time = 10.0
        let coordinator = KeyboardInputCoordinator(shiftClock: { time })
        let document = TestKeyboardDocument()

        coordinator.handle(testKey(.shift), document: document)
        time += 0.1
        coordinator.handle(testKey(.shift), document: document)
        type("xi", with: coordinator, into: document)

        #expect(document.text == "XI")
        #expect(coordinator.state.shiftState == .capsLocked)

        coordinator.handle(testKey(.shift), document: document)
        #expect(coordinator.state.shiftState == .lowercase)
    }

    @Test("Layout navigation switches between letters and symbol pages")
    func layoutNavigation() {
        let coordinator = KeyboardInputCoordinator()
        let document = TestKeyboardDocument()

        coordinator.handle(testKey(.symbols), document: document)
        #expect(coordinator.state.layoutMode == .symbolsPrimary)
        coordinator.handle(testKey(.moreSymbols), document: document)
        #expect(coordinator.state.layoutMode == .symbolsSecondary)
        coordinator.handle(testKey(.letters), document: document)
        #expect(coordinator.state.layoutMode == .letters)
    }
}
#endif
