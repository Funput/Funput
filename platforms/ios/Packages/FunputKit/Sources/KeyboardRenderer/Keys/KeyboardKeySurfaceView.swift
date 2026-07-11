#if canImport(UIKit)
import KeyboardLayout
import ThemeSchema
import UIKit

@MainActor
final class KeyboardKeySurfaceView: UIView {
    private var renderedView: UIView?
    private var normalAlpha: CGFloat = 1
    private var usesNativeInteraction = false

    func apply(
        theme: KeyboardThemeTokens,
        spec: KeySpec,
        traits: UITraitCollection,
        content: UIView
    ) {
        renderedView?.removeFromSuperview()

        let color = (spec.role.isSpecial ? theme.specialKey : theme.characterKey)
            .uiColor(for: traits)
        let opacity = spec.role.isSpecial ? theme.specialKeyOpacity : theme.keyOpacity
        normalAlpha = min(max(opacity, 0), 1)

        let result = makeSurface(
            theme: theme,
            spec: spec,
            color: color,
            opacity: normalAlpha
        )
        usesNativeInteraction = result.isNativeGlass
        configure(result.view, theme: theme, traits: traits)
        insertSubview(result.view, at: 0)
        result.contentView.addSubview(content)
        renderedView = result.view
        configureShadow(theme: theme)
        setNeedsLayout()
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        renderedView?.frame = bounds
    }

    func updateShape(cornerRadius: Double) {
        renderedView?.layer.cornerRadius = cornerRadius
        layer.shadowPath = UIBezierPath(
            roundedRect: bounds,
            cornerRadius: cornerRadius
        ).cgPath
    }

    func setPressed(_ pressed: Bool, theme: KeyboardThemeTokens, animated: Bool) {
        guard !usesNativeInteraction else { return }
        let updates = {
            self.transform = pressed
                ? CGAffineTransform(scaleX: theme.pressedScale, y: theme.pressedScale)
                : .identity
            self.renderedView?.alpha = pressed
                ? min(1, self.normalAlpha * theme.pressedOpacityMultiplier)
                : 1
        }
        guard animated, !UIAccessibility.isReduceMotionEnabled else {
            updates()
            return
        }
        UIView.animate(
            withDuration: pressed ? 0.07 : 0.13,
            delay: 0,
            options: [.allowUserInteraction, .beginFromCurrentState],
            animations: updates
        )
    }

    private func makeSurface(
        theme: KeyboardThemeTokens,
        spec: KeySpec,
        color: UIColor,
        opacity: CGFloat
    ) -> (view: UIView, contentView: UIView, isNativeGlass: Bool) {
        if #available(iOS 26.0, *),
           theme.material == .glass,
           !UIAccessibility.isReduceTransparencyEnabled {
            let effect = UIGlassEffect(style: .regular)
            effect.isInteractive = true
            effect.tintColor = color.withAlphaComponent(tintAlpha(for: spec, opacity: opacity))
            let view = UIVisualEffectView(effect: effect)
            return (view, view.contentView, true)
        }

        let view = UIView()
        view.backgroundColor = color.withAlphaComponent(opacity)
        return (view, view, false)
    }

    private func configure(
        _ view: UIView,
        theme: KeyboardThemeTokens,
        traits: UITraitCollection
    ) {
        view.layer.cornerCurve = .continuous
        view.layer.cornerRadius = theme.cornerRadius
        view.layer.borderWidth = usesNativeInteraction ? 0 : theme.borderWidth
        view.layer.borderColor = theme.border.uiColor(for: traits).cgColor
        view.clipsToBounds = true
    }

    private func configureShadow(theme: KeyboardThemeTokens) {
        layer.shadowColor = UIColor.black.cgColor
        layer.shadowOpacity = usesNativeInteraction ? 0 : Float(theme.shadowOpacity)
        layer.shadowRadius = usesNativeInteraction ? 0 : theme.shadowRadius
        layer.shadowOffset = CGSize(width: 0, height: 1)
    }

    private func tintAlpha(for spec: KeySpec, opacity: CGFloat) -> CGFloat {
        let base: CGFloat = spec.role.isSpecial ? 0.12 : 0.06
        let range: CGFloat = spec.role.isSpecial ? 0.26 : 0.22
        return base + opacity * range
    }
}
#endif
