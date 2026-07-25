#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import ThemeSchema
import UIKit

@MainActor
struct ThemeRendererColorTests {
    private let traits = UITraitCollection(userInterfaceStyle: .light)

    @Test("Bundled Glass stays neutral while custom background enables its gradient")
    func glassBackgroundOptIn() throws {
        guard #available(iOS 26, *) else { return }
        let backdrop = KeyboardBackdropView()
        backdrop.apply(theme: .funputGlass, traits: traits)
        let gradient = try #require(backdrop.contentView.layer.sublayers?
            .compactMap { $0 as? CAGradientLayer }.first)
        #expect(gradient.isHidden)

        var custom = ResolvedTheme.funputGlass
        custom.colorEffects.glassBackgroundTintEnabled = true
        backdrop.apply(theme: custom, traits: traits)
        #expect(!gradient.isHidden)
        #expect(gradient.colors?.count == 2)
    }

    @Test("Image background replaces gradient and applies adaptive overlay")
    func imageBackground() throws {
        var theme = ResolvedTheme.funputGlass
        theme.backgroundEffects = ThemeBackgroundEffects(
            mode: .image,
            image: ThemeBackgroundImage(assetID: "fixture", focalX: 0.2, zoom: 2),
            overlay: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0xFFFFFF, alpha: 0.35),
                dark: ThemeRGBA(hex: 0x000000, alpha: 0.6)
            )
        )
        let backdrop = KeyboardBackdropView()
        backdrop.frame = CGRect(x: 0, y: 0, width: 390, height: 300)
        backdrop.apply(theme: theme, traits: traits, image: UIImage(systemName: "photo"))
        backdrop.layoutIfNeeded()

        // The image, gradient and overlay sit inside the backdrop's rounded themed
        // container, which is stacked above the material view — not in the backdrop's
        // own subviews.
        let themed = try #require(backdrop.subviews.last)
        let image = try #require(themed.subviews.compactMap { $0 as? UIImageView }.first)
        #expect(!image.isHidden)
        #expect(backdrop.contentView.isHidden)
        #expect(themed.subviews.last?.backgroundColor?.cgColor.alpha == 0.35)
    }

    @Test("Glass key tint uses a bounded independent color layer")
    func glassKeyTint() throws {
        let tint = KeyboardKeyTintView()
        var theme = ResolvedTheme.funputGlass
        theme.characterKey = solid(0xFF0000)
        theme.colorEffects.glassKeyTintEnabled = true
        tint.apply(theme: theme, specIsSpecial: false, traits: traits, usesNativeGlass: true)

        let color = try #require(tint.subviews.first?.backgroundColor)
        var red: CGFloat = 0, alpha: CGFloat = 0
        color.getRed(&red, green: nil, blue: nil, alpha: &alpha)
        #expect(red > 0.99)
        #expect(abs(alpha - min(0.30, theme.keyOpacity * 0.30)) < 0.01)
    }

    @Test("Pressed overlay appears only while pressed")
    func pressedOverlayLifecycle() throws {
        let tint = KeyboardKeyTintView()
        var theme = ResolvedTheme.funputGlass
        theme.colorEffects.pressedOverlayEnabled = true
        theme.colorEffects.pressedOverlay = solid(0x00FF00)
        tint.apply(theme: theme, specIsSpecial: false, traits: traits, usesNativeGlass: true)
        let overlay = try #require(tint.subviews.last)

        #expect(overlay.alpha == 0)
        tint.setPressed(true)
        #expect(overlay.alpha == 1)
        #expect(overlay.backgroundColor?.cgColor.alpha == 0.18)
        tint.setPressed(false)
        #expect(overlay.alpha == 0)
    }

    @Test("Primary, secondary, and Enter content use their semantic colors")
    func contentColorRoles() throws {
        var theme = ResolvedTheme.funputGlass
        theme.label = solid(0xFF0000)
        theme.secondaryLabel = solid(0x00FF00)
        theme.accent = solid(0x0000FF)
        let presentation = KeyboardPresentation(theme: theme)
        let content = KeyboardKeyContentView()
        content.apply(
            spec: KeySpec(id: "hint", label: "1", role: .character, secondaryLabel: "´"),
            presentation: presentation,
            traits: traits
        )
        let labels = content.subviews.compactMap { $0 as? UILabel }
        let primary = try #require(labels.first { $0.text == "1" })
        let secondary = try #require(labels.first { $0.text == "´" })
        #expect(primary.textColor.isClose(to: UIColor(red: 1, green: 0, blue: 0, alpha: 1)))
        #expect(secondary.textColor.isClose(to: UIColor(red: 0, green: 1, blue: 0, alpha: 1)))

        content.apply(
            spec: KeySpec(id: "enter", label: "Enter", role: .enter),
            presentation: presentation,
            traits: traits
        )
        let icon = try #require(content.subviews.compactMap { $0 as? UIImageView }
            .first { !$0.isHidden })
        #expect(icon.tintColor.isClose(to: UIColor(red: 0, green: 0, blue: 1, alpha: 1)))
    }

    private func solid(_ hex: UInt32) -> AdaptiveThemeColor {
        let color = ThemeRGBA(hex: hex)
        return AdaptiveThemeColor(light: color, dark: color)
    }
}

private extension UIColor {
    func isClose(to other: UIColor) -> Bool {
        var ar: CGFloat = 0, ag: CGFloat = 0, ab: CGFloat = 0, aa: CGFloat = 0
        var br: CGFloat = 0, bg: CGFloat = 0, bb: CGFloat = 0, ba: CGFloat = 0
        getRed(&ar, green: &ag, blue: &ab, alpha: &aa)
        other.getRed(&br, green: &bg, blue: &bb, alpha: &ba)
        let a = [ar, ag, ab, aa]
        let b = [br, bg, bb, ba]
        return zip(a, b).allSatisfy { abs($0 - $1) < 0.01 }
    }
}
#endif
