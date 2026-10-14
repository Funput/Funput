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

    @Test("Corner keys show their word, and return names what it does")
    func cornerLabels() {
        #expect(KeyboardKeyContentStyle.cornerLabel(for: key(.enter, "return"), enterAction: .newLine) == "return")
        #expect(KeyboardKeyContentStyle.cornerLabel(for: key(.enter, "return"), enterAction: .search) == "search")
        #expect(KeyboardKeyContentStyle.cornerLabel(for: key(.enter, "return"), enterAction: .custom("Gửi")) == "Gửi")
        #expect(KeyboardKeyContentStyle.cornerLabel(for: key(.backspace, "delete"), enterAction: .go) == "delete")
        #expect(KeyboardKeyContentStyle.isLit(.shift, shiftState: .uppercase))
        #expect(!KeyboardKeyContentStyle.isLit(.capsLock, shiftState: .uppercase))
        #expect(KeyboardKeyContentStyle.isLit(.capsLock, shiftState: .capsLocked))
    }

    @Test("A corner key draws its word in the corner, with no icon")
    func cornerContent() throws {
        let view = KeyboardKeyContentView(frame: CGRect(x: 0, y: 0, width: 110, height: 64))
        let spec = KeySpec(id: "delete", label: "delete", role: .backspace, labelPlacement: .bottomTrailing)
        view.apply(spec: spec, presentation: KeyboardPresentation(), traits: UITraitCollection())
        view.layoutIfNeeded()
        let label = try #require(view.subviews.compactMap { $0 as? UILabel }.first { $0.text == "delete" })
        let icon = try #require(view.subviews.compactMap { $0 as? UIImageView }.first)
        #expect(!label.isHidden && label.textAlignment == .right)
        #expect(label.frame.midY > view.bounds.midY)
        #expect(icon.isHidden)
    }

    @Test("A letter key shares its baseline with the corner words and centers its hint above")
    func bottomCenterContent() throws {
        let bounds = CGRect(x: 0, y: 0, width: 78, height: 64)
        let letter = KeyboardKeyContentGeometry.bottomCenterLabel(in: bounds, lineHeight: 26)
        let corner = KeyboardKeyContentGeometry.cornerLabel(in: bounds, lineHeight: 26)
        #expect(letter.maxY == corner.maxY)
        #expect(letter.midY > bounds.midY)

        let view = KeyboardKeyContentView(frame: bounds)
        let spec = KeySpec(id: "q", label: "q", secondaryLabel: "1", role: .character, labelPlacement: .bottomCenter)
        view.apply(spec: spec, presentation: KeyboardPresentation(), traits: UITraitCollection())
        view.layoutIfNeeded()
        let labels = view.subviews.compactMap { $0 as? UILabel }
        let hint = try #require(labels.first { $0.text == "1" })
        #expect(hint.textAlignment == .center)
        #expect(try #require(labels.first { $0.text == "q" }).textAlignment == .center)
    }

    @Test("The globe, emoji and hide keys keep a small symbol on a tall key")
    func fixedSizeIcons() {
        let bounds = CGRect(x: 0, y: 0, width: 110, height: 90)
        let fallback = bounds.insetBy(dx: 18, dy: 18)
        for role in [KeyRole.globe, .emoji, .dismissKeyboard] {
            let frame = KeyboardKeyIconSizing.frame(for: role, in: bounds, fallback: fallback)
            #expect(frame.width == KeyboardKeyIconSizing.symbolSide && frame.height == frame.width)
            #expect(frame.midX == bounds.midX && frame.midY == bounds.midY)
        }
        #expect(KeyboardKeyIconSizing.frame(for: .shift, in: bounds, fallback: fallback) == fallback)
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

    private func key(_ role: KeyRole, _ label: String) -> KeySpec {
        KeySpec(id: label, label: label, role: role, labelPlacement: .bottomTrailing)
    }

    private func surface(
        for mode: KeyboardLayoutMode,
        formFactor: KeyboardFormFactor = .pad
    ) -> KeyboardSurfaceView {
        let layout = KeyboardLayoutResolver.resolve(
            inputMethod: .telex, mode: mode, preset: .system, formFactor: formFactor
        )
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
