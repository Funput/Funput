@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import ThemeSchema
import UIKit

@MainActor
struct ThemeRendererSurfaceTests {
    private let traits = UITraitCollection(userInterfaceStyle: .light)

    @Test("Solid backdrop has no blur and an opaque gradient")
    func solidBackdrop() throws {
        var theme = ResolvedTheme.funputGlass
        theme.material = .solid
        let backdrop = KeyboardBackdropView()
        backdrop.apply(theme: theme, traits: traits)
        let gradient = try #require(backdrop.contentView.layer.sublayers?
            .compactMap { $0 as? CAGradientLayer }.first)

        #expect(backdrop.effect == nil)
        #expect(gradient.colors?.allSatisfy { ($0 as! CGColor).alpha == 1 } == true)
    }

    @Test("Translucent backdrop uses a standard blur")
    func translucentBackdrop() {
        var theme = ResolvedTheme.funputGlass
        theme.material = .translucent
        let backdrop = KeyboardBackdropView()
        backdrop.apply(theme: theme, traits: traits)

        #expect(backdrop.effect is UIBlurEffect)
        #expect(!backdrop.usesHostMaterial)
    }

    @Test("Backdrop applies every authored gradient direction", arguments: ThemeGradientDirection.allCases)
    func gradientDirection(_ direction: ThemeGradientDirection) throws {
        var theme = ResolvedTheme.funputGlass
        theme.material = .translucent
        theme.gradientDirection = direction
        let backdrop = KeyboardBackdropView()
        backdrop.apply(theme: theme, traits: traits)
        let gradient = try #require(backdrop.contentView.layer.sublayers?
            .compactMap { $0 as? CAGradientLayer }.first)
        let expected = direction.layerPoints

        #expect(gradient.startPoint == expected.start)
        #expect(gradient.endPoint == expected.end)
    }

    @Test("Glass border and shadow require explicit overrides")
    func glassOverrides() throws {
        guard #available(iOS 26, *) else { return }
        let surface = KeyboardKeySurfaceView()
        surface.frame = CGRect(x: 0, y: 0, width: 40, height: 44)
        var theme = ResolvedTheme.funputGlass
        apply(theme, to: surface)
        surface.layoutIfNeeded()
        let border = try #require(surface.layer.sublayers?
            .compactMap { $0 as? KeyboardKeyBorderLayer }.first)
        #expect(border.isHidden)
        #expect(surface.layer.shadowOpacity == 0)

        theme.surfaceEffects.glassBorderOverrideEnabled = true
        theme.surfaceEffects.glassShadowOverrideEnabled = true
        apply(theme, to: surface)
        surface.layoutIfNeeded()
        #expect(!border.isHidden)
        #expect(abs(border.lineWidth - theme.borderWidth) < 0.001)
        #expect(border.path != nil)
        #expect(surface.layer.shadowOpacity == Float(theme.shadowOpacity))
        #expect(surface.bounds == CGRect(x: 0, y: 0, width: 40, height: 44))
    }

    @Test("Core Animation can copy the custom border layer")
    func borderLayerCopy() {
        let source = KeyboardKeyBorderLayer()
        var theme = ResolvedTheme.funputGlass
        theme.surfaceEffects.glassBorderOverrideEnabled = true
        source.apply(theme: theme, traits: traits, usesNativeGlass: true)
        source.update(in: CGRect(x: 0, y: 0, width: 40, height: 44))

        let copy = KeyboardKeyBorderLayer(layer: source)
        copy.update(in: CGRect(x: 0, y: 0, width: 50, height: 48))

        #expect(copy.path != nil)
        #expect(abs(copy.lineWidth - source.lineWidth) < 0.001)
    }

    @Test("Flat key surfaces honor semantic opacity")
    func flatKeyOpacity() throws {
        var theme = ResolvedTheme.funputGlass
        theme.material = .solid
        theme.keyOpacity = 0.4
        theme.specialKeyOpacity = 0.7
        let surface = KeyboardKeySurfaceView()
        apply(theme, to: surface)
        let color = try #require(surface.subviews.first?.backgroundColor)
        #expect(abs(color.cgColor.alpha - 0.4) < 0.01)

        surface.apply(
            theme: theme,
            spec: KeySpec(id: "enter", label: "Enter", role: .enter),
            traits: traits,
            content: UIView()
        )
        let special = try #require(surface.subviews.first?.backgroundColor)
        #expect(abs(special.cgColor.alpha - 0.7) < 0.01)
    }

    @Test("Key preview surface follows selected material")
    func previewMaterial() {
        var theme = ResolvedTheme.funputGlass
        theme.material = .solid
        let solid = KeyboardKeyPreviewSurfaceFactory.make(
            theme: theme, traits: traits, reducesTransparency: false
        )
        theme.material = .translucent
        let translucent = KeyboardKeyPreviewSurfaceFactory.make(
            theme: theme, traits: traits, reducesTransparency: false
        )

        #expect(!(solid.view is UIVisualEffectView))
        #expect(translucent.view is UIVisualEffectView)
    }

    private func apply(_ theme: ResolvedTheme, to surface: KeyboardKeySurfaceView) {
        surface.apply(
            theme: theme,
            spec: KeySpec(id: "a", label: "a", role: .character),
            traits: traits,
            content: UIView()
        )
    }
}
