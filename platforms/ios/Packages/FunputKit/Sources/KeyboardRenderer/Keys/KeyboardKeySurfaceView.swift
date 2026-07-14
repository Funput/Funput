#if canImport(UIKit)
import KeyboardLayout
import ThemeSchema
import UIKit

@MainActor
final class KeyboardKeySurfaceView: UIView {
    private var renderedView: UIView?
    private var normalAlpha: CGFloat = 1
    private var usesNativeInteraction = false
    private let tintView = KeyboardKeyTintView()

    func apply(
        theme: ResolvedTheme,
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
            color: color,
            opacity: normalAlpha
        )
        usesNativeInteraction = result.isNativeGlass
        configure(result.view, theme: theme, traits: traits)
        insertSubview(result.view, at: 0)
        result.contentView.addSubview(tintView)
        result.contentView.addSubview(content)
        result.contentView.isUserInteractionEnabled = true
        tintView.apply(
            theme: theme,
            specIsSpecial: spec.role.isSpecial,
            traits: traits,
            usesNativeGlass: usesNativeInteraction
        )
        tintView.setPressed(false)
        renderedView = result.view
        configureShadow(theme: theme)
        setNeedsLayout()
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        renderedView?.frame = bounds
        tintView.frame = tintView.superview?.bounds ?? bounds
    }

    func updateShape(cornerRadius: Double) {
        if #available(iOS 26.0, *), usesNativeInteraction {
            renderedView?.cornerConfiguration = .corners(radius: .fixed(cornerRadius))
        } else {
            renderedView?.layer.cornerRadius = cornerRadius
        }
        layer.shadowPath = UIBezierPath(
            roundedRect: bounds,
            cornerRadius: cornerRadius
        ).cgPath
    }

    func setPressed(_ pressed: Bool, theme: ResolvedTheme, animated: Bool) {
        let updates = {
            self.transform = pressed
                ? CGAffineTransform(scaleX: theme.pressedScale, y: theme.pressedScale)
                : .identity
            self.tintView.setPressed(pressed)
            self.renderedView?.alpha = self.tintView.hasPressedOverlay
                ? 1
                : (pressed ? min(1, self.normalAlpha * theme.pressedOpacityMultiplier) : 1)
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
        theme: ResolvedTheme,
        color: UIColor,
        opacity: CGFloat
    ) -> (view: UIView, contentView: UIView, isNativeGlass: Bool) {
        if #available(iOS 26.0, *),
           theme.material == .glass,
           !UIAccessibility.isReduceTransparencyEnabled {
            let effect = UIGlassEffect(style: .regular)
            effect.isInteractive = false
            // A glass container shares adaptation across its children. Keep every
            // key's material neutral so a prominent key cannot tint the group after
            // the keyboard extension is deactivated and activated again.
            effect.tintColor = .clear
            let view = UIVisualEffectView(effect: effect)
            view.tintColor = .clear
            return (view, view.contentView, true)
        }

        let view = UIView()
        view.backgroundColor = color.withAlphaComponent(opacity)
        return (view, view, false)
    }

    private func configure(
        _ view: UIView,
        theme: ResolvedTheme,
        traits: UITraitCollection
    ) {
        if #available(iOS 26.0, *), usesNativeInteraction {
            view.cornerConfiguration = .corners(radius: .fixed(theme.cornerRadius))
            view.clipsToBounds = false
        } else {
            view.layer.cornerCurve = .continuous
            view.layer.cornerRadius = theme.cornerRadius
            view.clipsToBounds = true
        }
        view.layer.borderWidth = usesNativeInteraction ? 0 : theme.borderWidth
        view.layer.borderColor = theme.border.uiColor(for: traits).cgColor
    }

    private func configureShadow(theme: ResolvedTheme) {
        layer.shadowColor = UIColor.black.cgColor
        layer.shadowOpacity = usesNativeInteraction ? 0 : Float(theme.shadowOpacity)
        layer.shadowRadius = usesNativeInteraction ? 0 : theme.shadowRadius
        layer.shadowOffset = CGSize(width: 0, height: 1)
    }

}
#endif
