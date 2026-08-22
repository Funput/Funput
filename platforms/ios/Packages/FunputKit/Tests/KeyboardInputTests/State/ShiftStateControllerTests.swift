#if os(iOS) && canImport(FunputCore)
@testable import KeyboardInput
import KeyboardLayout
import Testing

@MainActor
struct ShiftStateControllerTests {
    @Test("Double-tapping from lowercase enables Caps Lock")
    func doubleTapFromLowercase() {
        var now: Double = 0
        var controller = ShiftStateController(doubleTapInterval: 0.3) { now }

        let first = controller.toggle(from: .lowercase)
        now = 0.1
        let second = controller.toggle(from: first)

        #expect(first == .uppercase)
        #expect(second == .capsLocked)
    }

    @Test("Double-tapping from uppercase enables Caps Lock")
    func doubleTapFromUppercase() {
        var now: Double = 0
        var controller = ShiftStateController(doubleTapInterval: 0.3) { now }

        let first = controller.toggle(from: .uppercase)
        now = 0.1
        let second = controller.toggle(from: first)

        #expect(first == .lowercase)
        #expect(second == .capsLocked)
    }

    @Test("A delayed second tap still toggles instead of locking")
    func outsideWindowToggles() {
        var now: Double = 0
        var controller = ShiftStateController(doubleTapInterval: 0.3) { now }

        let first = controller.toggle(from: .uppercase)
        now = 0.4
        let second = controller.toggle(from: first)

        #expect(first == .lowercase)
        #expect(second == .uppercase)
    }

    @Test("Tapping Shift while Caps Lock is on turns it off")
    func capsLockReleases() {
        var now: Double = 0
        var controller = ShiftStateController(doubleTapInterval: 0.3) { now }

        _ = controller.toggle(from: .lowercase)
        now = 0.1
        let locked = controller.toggle(from: .uppercase)
        now = 0.15
        let released = controller.toggle(from: locked)

        #expect(locked == .capsLocked)
        #expect(released == .lowercase)
    }
}
#endif
