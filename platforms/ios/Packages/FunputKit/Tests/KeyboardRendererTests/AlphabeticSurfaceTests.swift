#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import UIKit

@MainActor
struct AlphabeticSurfaceTests {
    @Test("VNI alphabetic layout renders five usable rows")
    func vniSurface() {
        let layout = StandardKeyboardLayouts.letters(.vni)
        let surface = KeyboardSurfaceView(presentation: KeyboardPresentation(layout: layout))
        surface.frame = CGRect(x: 0, y: 0, width: 390, height: 304)
        surface.layoutIfNeeded()

        let keys = accessibleControls(in: surface)
        #expect(layout.rows.count == 5)
        #expect(keys.filter { $0.accessibilityLabel?.count == 1 }.count >= 26)
        #expect(keys.allSatisfy { !$0.frame.isEmpty })
    }

    @Test("Shift icon follows presentation state")
    func shiftIcon() {
        let lower = KeyboardKeyContentStyle.icon(for: .shift, shiftState: .lowercase)
        let upper = KeyboardKeyContentStyle.icon(for: .shift, shiftState: .uppercase)
        let locked = KeyboardKeyContentStyle.icon(for: .shift, shiftState: .capsLocked)

        #expect(lower?.isEqual(UIImage(systemName: "shift")) == true)
        #expect(upper?.isEqual(UIImage(systemName: "shift.fill")) == true)
        #expect(locked?.isEqual(UIImage(systemName: "capslock.fill")) == true)
    }

    /// One overlay receives every touch (so a finger can slide between keys), and the
    /// key is resolved from geometry; keycap controls only render and carry a11y.
    @Test("A touch on a keycap is routed to that key through the touch overlay")
    func characterHitTesting() throws {
        let layout = StandardKeyboardLayouts.letters(.vni)
        let surface = KeyboardSurfaceView(presentation: KeyboardPresentation(layout: layout))
        surface.frame = CGRect(x: 0, y: 0, width: 390, height: 304)
        surface.layoutIfNeeded()

        let keys = accessibleControls(in: surface)
        let key = try #require(keys.first { $0.accessibilityLabel == "a" })
        let point = key.convert(CGPoint(x: key.bounds.midX, y: key.bounds.midY), to: surface)

        // The overlay is the topmost interactive view there, and it resolves the point
        // back to the key the user aimed at.
        #expect(surface.hitTest(point, with: nil) === surface.touchOverlay)
        #expect(surface.touchOverlay.resolvedHit(at: point)?.key.id == "character-a")
    }

    @Test("Shift preserves Liquid Glass surfaces")
    func shiftPreservesGlass() {
        guard #available(iOS 26.0, *) else { return }
        let layout = StandardKeyboardLayouts.letters(.vni)
        let surface = KeyboardSurfaceView(presentation: KeyboardPresentation(layout: layout))
        surface.frame = CGRect(x: 0, y: 0, width: 390, height: 304)
        surface.layoutIfNeeded()
        let originalEffects = Set(visualEffects(in: surface).map(ObjectIdentifier.init))

        var presentation = surface.presentation
        presentation.shiftState = .uppercase
        surface.presentation = presentation
        surface.layoutIfNeeded()
        let updatedEffects = Set(visualEffects(in: surface).map(ObjectIdentifier.init))

        #expect(!originalEffects.isEmpty)
        #expect(updatedEffects == originalEffects)
    }

    @Test("Shift updates content without rebuilding key controls")
    func shiftPreservesKeyControls() {
        let layout = StandardKeyboardLayouts.letters(.vni)
        let surface = KeyboardSurfaceView(presentation: KeyboardPresentation(layout: layout))
        surface.frame = CGRect(x: 0, y: 0, width: 390, height: 304)
        surface.layoutIfNeeded()
        let before = Set(accessibleControls(in: surface).map(ObjectIdentifier.init))

        var presentation = surface.presentation
        presentation.shiftState = .uppercase
        surface.presentation = presentation
        surface.layoutIfNeeded()
        let after = Set(accessibleControls(in: surface).map(ObjectIdentifier.init))

        #expect(before == after)
    }

    @Test("Liquid Glass keys use native adaptive styling")
    func nativeGlassStyling() {
        guard #available(iOS 26.0, *) else { return }
        let layout = StandardKeyboardLayouts.letters(.telex)
        let surface = KeyboardSurfaceView(presentation: KeyboardPresentation(layout: layout))
        surface.frame = CGRect(x: 0, y: 0, width: 390, height: 304)
        surface.layoutIfNeeded()

        let letter = accessibleControls(in: surface).first { $0.accessibilityLabel == "a" }!
        let enter = accessibleControls(in: surface).first { $0.accessibilityLabel == "Enter" }!
        let letterEffect = glassEffects(in: letter).first
        let enterEffect = glassEffects(in: enter).first
        let containerEffect = visualEffects(in: surface)
            .compactMap { $0.effect as? UIGlassContainerEffect }
            .first

        #expect(letterEffect?.isInteractive == true)
        #expect(letterEffect?.tintColor?.cgColor.alpha == 0)
        #expect(enterEffect?.tintColor?.cgColor.alpha == 0)
        #expect(containerEffect?.spacing == 0)
        #expect(glassViews(in: letter).first?.cornerConfiguration == .corners(radius: .fixed(6)))
    }

    @Test("Liquid Glass uses the keyboard host material as its backdrop")
    func nativeGlassBackdrop() {
        guard #available(iOS 26.0, *) else { return }
        let surface = KeyboardSurfaceView()
        let backdrop = surface.subviews.compactMap { $0 as? KeyboardBackdropView }.first

        #expect(backdrop?.usesHostMaterial == true)
        #expect(backdrop?.effect == nil)
        #expect(backdrop?.backgroundColor == .clear)
        #expect(backdrop?.isOpaque == false)
    }

    private func accessibleControls(in view: UIView) -> [UIControl] {
        view.subviews.flatMap { child -> [UIControl] in
            let own = (child as? UIControl).map {
                $0.isAccessibilityElement && !$0.isHidden ? [$0] : []
            } ?? []
            return own + accessibleControls(in: child)
        }
    }

    private func visualEffects(in view: UIView) -> [UIVisualEffectView] {
        view.subviews.flatMap { child in
            let own = (child as? UIVisualEffectView).map { [$0] } ?? []
            return own + visualEffects(in: child)
        }
    }

    @available(iOS 26.0, *)
    private func glassEffects(in view: UIView) -> [UIGlassEffect] {
        visualEffects(in: view).compactMap { $0.effect as? UIGlassEffect }
    }

    @available(iOS 26.0, *)
    private func glassViews(in view: UIView) -> [UIVisualEffectView] {
        visualEffects(in: view).filter { $0.effect is UIGlassEffect }
    }
}
#endif
