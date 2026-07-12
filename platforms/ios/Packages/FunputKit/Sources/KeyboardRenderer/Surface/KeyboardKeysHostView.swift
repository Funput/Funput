#if canImport(UIKit)
import KeyboardLayout
import ThemeSchema
import UIKit

@MainActor
final class KeyboardKeysHostView: UIView {
    private var contentHost = UIView()
    private var glassContainerView: UIVisualEffectView?
    private var usesGlassContainer = false
    private var controls: [KeyboardKeyControl] = []

    func install(_ controls: [KeyboardKeyControl]) {
        self.controls.forEach { $0.removeFromSuperview() }
        self.controls = controls
        controls.forEach(contentHost.addSubview)
    }

    func apply(presentation: KeyboardPresentation) {
        let shouldUseGlass = shouldUseGlass(for: presentation.theme)
        guard contentHost.superview == nil || shouldUseGlass != usesGlassContainer else {
            updateSpacing()
            return
        }
        rebuildHost(useGlass: shouldUseGlass)
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        glassContainerView?.frame = bounds
        if glassContainerView == nil {
            contentHost.frame = bounds
        }
    }

    private func rebuildHost(useGlass: Bool) {
        glassContainerView?.removeFromSuperview()
        if glassContainerView == nil {
            contentHost.removeFromSuperview()
        }

        if useGlass, #available(iOS 26.0, *) {
            let effect = UIGlassContainerEffect()
            effect.spacing = containerSpacing
            let container = UIVisualEffectView(effect: effect)
            container.tintColor = .clear
            addSubview(container)
            glassContainerView = container
            contentHost = container.contentView
            contentHost.isUserInteractionEnabled = true
        } else {
            let host = UIView()
            addSubview(host)
            glassContainerView = nil
            contentHost = host
        }

        controls.forEach(contentHost.addSubview)
        usesGlassContainer = useGlass
        setNeedsLayout()
    }

    private func shouldUseGlass(for theme: KeyboardThemeTokens) -> Bool {
        guard theme.material == .glass,
              !UIAccessibility.isReduceTransparencyEnabled else { return false }
        if #available(iOS 26.0, *) { return true }
        return false
    }

    private var containerSpacing: CGFloat {
        // Keys have a stable layout and should remain distinct. The container still
        // gives every glass surface one sampling region and consistent adaptation.
        0
    }

    private func updateSpacing() {
        if #available(iOS 26.0, *),
           let effect = glassContainerView?.effect as? UIGlassContainerEffect {
            effect.spacing = containerSpacing
        }
    }
}
#endif
