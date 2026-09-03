#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import UIKit

/// The Liquid Glass path only exists on iOS 26+, so these bodies return early on
/// anything older — and `test-funput-kit.sh` picks the first available iOS simulator,
/// which is where two stale assertions here survived unnoticed for weeks. Run them on
/// an iOS 26 simulator when touching the glass surfaces.
@MainActor
struct LiquidGlassSurfaceTests {
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

        // Non-interactive on purpose: the keycap runs its own press animation
        // (scale plus the tint overlay in `KeyboardKeySurfaceView.setPressed`), and
        // native interactive glass would answer the same touch a second time.
        #expect(letterEffect?.isInteractive == false)
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
        #expect(backdrop?.isOpaque == false)
        // Nothing of its own painted over the host material. Asserted by alpha rather
        // than against `.clear`, which the backdrop has never actually been set to —
        // an unset background is just as transparent.
        #expect((backdrop?.backgroundColor?.cgColor.alpha ?? 0) == 0)
    }

    /// A pinned appearance cannot borrow the host app's glass: that material stays on the
    /// host app's own light/dark setting, so a keyboard pinned to dark inside a light app
    /// would end up drawing dark-mode labels onto light glass.
    @Test("A pinned appearance brings its own material instead of the host's")
    func pinnedAppearanceDropsHostMaterial() {
        guard #available(iOS 26.0, *) else { return }
        let surface = KeyboardSurfaceView(
            presentation: KeyboardPresentation(pinsAppearance: true)
        )
        let backdrop = surface.subviews.compactMap { $0 as? KeyboardBackdropView }.first

        #expect(backdrop?.usesHostMaterial == false)
        #expect(backdrop?.effect is UIBlurEffect)
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
