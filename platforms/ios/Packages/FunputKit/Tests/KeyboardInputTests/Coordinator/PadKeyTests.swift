#if os(iOS) && canImport(FunputCore)
import KeyboardInput
import KeyboardLayout
import Testing

/// The iPad's tab and caps lock keys.
@MainActor
struct PadKeyTests {
    @Test("Tab types a tab character and ends the word being composed")
    func tabEndsWord() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let document = TestKeyboardWriter()

        type("as", with: coordinator, into: document)
        coordinator.handle(testKey(.tab), writer: document)
        type("s", with: coordinator, into: document)

        #expect(document.text == "á\ts")
    }

    @Test("Caps lock locks with one tap and unlocks with the next")
    func capsLockToggles() {
        let coordinator = KeyboardInputCoordinator()
        let document = TestKeyboardWriter()

        coordinator.handle(testKey(.capsLock), writer: document)
        type("xi", with: coordinator, into: document)
        #expect(document.text == "XI")
        #expect(coordinator.state.shiftState == .capsLocked)

        coordinator.handle(testKey(.capsLock), writer: document)
        type("n", with: coordinator, into: document)
        #expect(document.text == "XIn")
        #expect(coordinator.state.shiftState == .lowercase)
    }

    @Test("Caps lock locks from an armed Shift, and Shift releases the lock")
    func capsLockAndShift() {
        let coordinator = KeyboardInputCoordinator()
        let document = TestKeyboardWriter()

        coordinator.handle(testKey(.shift), writer: document)
        coordinator.handle(testKey(.capsLock), writer: document)
        #expect(coordinator.state.shiftState == .capsLocked)

        coordinator.handle(testKey(.shift), writer: document)
        #expect(coordinator.state.shiftState == .lowercase)
    }
}
#endif
