#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import UIKit

@MainActor
struct PadSurfaceTests {
    @Test("The caps lock key fills while the lock is on")
    func capsLockIcon() {
        let off = KeyboardKeyContentStyle.icon(for: .capsLock, shiftState: .uppercase)
        let on = KeyboardKeyContentStyle.icon(for: .capsLock, shiftState: .capsLocked)
        #expect(off?.isEqual(UIImage(systemName: "capslock")) == true)
        #expect(on?.isEqual(UIImage(systemName: "capslock.fill")) == true)
    }

    @Test("The input mode control covers the globe key once someone listens")
    func inputModeSwitchOverGlobe() {
        let surface = surface(for: .letters)
        #expect(surface.inputModeSwitchControl.isHidden)

        let target = Target()
        surface.setInputModeSwitch(target: target, action: #selector(Target.handle))
        surface.layoutIfNeeded()

        let globe = surface.keyControls["globe-pad"]
        #expect(!surface.inputModeSwitchControl.isHidden)
        #expect(globe.map { surface.inputModeSwitchControl.frame == $0.frame } == true)
        #expect(surface.inputModeSwitchControl.superview === surface.touchOverlay.superview)
        let siblings = surface.contentHost.subviews
        #expect(siblings.firstIndex(of: surface.inputModeSwitchControl)!
            > siblings.firstIndex(of: surface.touchOverlay)!)
    }

    @Test("A page without a globe hides the input mode control")
    func noGlobe() {
        let surface = surface(for: .letters, formFactor: .phone)
        let target = Target()
        surface.setInputModeSwitch(target: target, action: #selector(Target.handle))
        surface.layoutIfNeeded()
        #expect(surface.inputModeSwitchControl.isHidden)
    }

    private func surface(
        for mode: KeyboardLayoutMode,
        formFactor: KeyboardFormFactor = .pad
    ) -> KeyboardSurfaceView {
        let layout = KeyboardLayoutResolver.resolve(inputMethod: .telex, mode: mode, formFactor: formFactor)
        let surface = KeyboardSurfaceView(presentation: KeyboardPresentation(layout: layout))
        surface.frame = CGRect(x: 0, y: 0, width: 1024, height: 400)
        surface.layoutIfNeeded()
        return surface
    }
}

private final class Target: NSObject {
    @objc func handle() {}
}
#endif
