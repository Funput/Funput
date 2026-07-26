#if canImport(UIKit)
import ThemeSchema
import UIKit

@MainActor
enum KeyboardKeyPreviewSurfaceFactory {
    static func make(
        theme: ResolvedTheme,
        traits: UITraitCollection,
        reducesTransparency: Bool
    ) -> (view: UIView, usesNativeGlass: Bool) {
        if #available(iOS 26.0, *),
           theme.material == .glass,
           !reducesTransparency {
            let glass = UIGlassEffect(style: .regular)
            glass.isInteractive = false
            glass.tintColor = .clear
            let view = UIVisualEffectView(effect: glass)
            view.tintColor = .clear
            return (view, true)
        }

        if theme.material == .solid || reducesTransparency {
            let view = UIView()
            view.backgroundColor = theme.characterKey.uiColor(for: traits)
                .withAlphaComponent(theme.keyOpacity)
            return (view, false)
        }

        return (UIVisualEffectView(effect: UIBlurEffect(style: .systemMaterial)), false)
    }
}
#endif
