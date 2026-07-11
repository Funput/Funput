#if canImport(UIKit)
import KeyboardLayout
import ThemeSchema
import UIKit

@MainActor
final class KeyboardKeyControl: UIControl {
    var onEvent: ((KeyboardKeyEvent) -> Void)?

    private let spec: KeySpec
    private let interactionControl = UIControl()
    private let label = UILabel()
    private let iconView = UIImageView()
    private var surfaceView: UIView?
    private var theme = KeyboardThemeTokens.funputGlass
    private var shiftState = ShiftState.lowercase
    private var normalSurfaceAlpha: CGFloat = 1
    private var usesNativeGlassInteraction = false

    init(spec: KeySpec) {
        self.spec = spec
        super.init(frame: .zero)

        isAccessibilityElement = true
        accessibilityLabel = spec.accessibilityLabel
        accessibilityTraits = .keyboardKey
        clipsToBounds = false

        interactionControl.isAccessibilityElement = false
        interactionControl.addAction(UIAction { [weak self] _ in
            self?.handleTouchDown()
        }, for: .touchDown)
        interactionControl.addAction(UIAction { [weak self] _ in
            self?.handleTouchUpInside()
        }, for: .touchUpInside)
        interactionControl.addAction(UIAction { [weak self] _ in
            self?.handleTouchCancelled()
        }, for: [.touchCancel, .touchUpOutside])

        label.textAlignment = .center
        label.adjustsFontSizeToFitWidth = true
        label.minimumScaleFactor = 0.7
        label.isUserInteractionEnabled = false
        interactionControl.addSubview(label)

        iconView.contentMode = .scaleAspectFit
        iconView.preferredSymbolConfiguration = UIImage.SymbolConfiguration(weight: .medium)
        iconView.isUserInteractionEnabled = false
        interactionControl.addSubview(iconView)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        surfaceView?.frame = bounds
        surfaceView?.layer.cornerRadius = CGFloat(theme.cornerRadius)
        interactionControl.frame = bounds

        let contentInset = max(6, bounds.height * 0.2)
        label.frame = bounds.insetBy(dx: 5, dy: contentInset * 0.5)
        iconView.frame = bounds.insetBy(dx: contentInset, dy: contentInset)
        layer.shadowPath = UIBezierPath(
            roundedRect: bounds,
            cornerRadius: CGFloat(theme.cornerRadius)
        ).cgPath
    }

    func apply(theme: KeyboardThemeTokens, shiftState: ShiftState, traits: UITraitCollection) {
        self.theme = theme
        self.shiftState = shiftState

        let surfaceColor = (spec.role.isSpecial ? theme.specialKey : theme.characterKey)
            .uiColor(for: traits)
        let opacity = spec.role.isSpecial ? theme.specialKeyOpacity : theme.keyOpacity
        normalSurfaceAlpha = CGFloat(min(max(opacity, 0), 1))
        installSurface(color: surfaceColor, opacity: normalSurfaceAlpha)

        let labelColor = theme.label.uiColor(for: traits)
        label.textColor = spec.role == .space
            ? theme.secondaryLabel.uiColor(for: traits)
            : labelColor
        iconView.tintColor = labelColor
        label.font = font(for: spec.role, scale: theme.fontScale)
        label.text = displayedLabel
        iconView.image = iconName.flatMap { UIImage(systemName: $0) }
        label.isHidden = iconView.image != nil
        iconView.isHidden = iconView.image == nil

        layer.shadowColor = UIColor.black.cgColor
        layer.shadowOpacity = usesNativeGlassInteraction ? 0 : Float(theme.shadowOpacity)
        layer.shadowRadius = usesNativeGlassInteraction ? 0 : CGFloat(theme.shadowRadius)
        layer.shadowOffset = CGSize(width: 0, height: 1)
        setNeedsLayout()
    }

    override func accessibilityActivate() -> Bool {
        onEvent?(KeyboardKeyEvent(key: spec, phase: .pressed))
        onEvent?(KeyboardKeyEvent(key: spec, phase: .released))
        return true
    }

    private var displayedLabel: String {
        if spec.role == .character, shiftState.isUppercase {
            return spec.shiftedLabel ?? spec.label.uppercased()
        }
        return spec.label
    }

    private var iconName: String? {
        switch spec.role {
        case .shift:
            shiftState.isUppercase ? "shift.fill" : "shift"
        case .backspace:
            "delete.left"
        case .inputMode:
            "globe"
        case .enter:
            "return"
        case .emoji:
            "face.smiling"
        case .settings:
            "gearshape"
        default:
            nil
        }
    }

    private func font(for role: KeyRole, scale: Double) -> UIFont {
        let pointSize: CGFloat
        let weight: UIFont.Weight
        switch role {
        case .character, .punctuation:
            pointSize = 22
            weight = .regular
        case .space:
            pointSize = 12
            weight = .medium
        default:
            pointSize = 14
            weight = .semibold
        }
        return .systemFont(ofSize: pointSize * scale, weight: weight)
    }

    private func installSurface(color: UIColor, opacity: CGFloat) {
        surfaceView?.removeFromSuperview()

        let view: UIView
        let contentView: UIView
        if #available(iOS 26.0, *),
           theme.material == .glass,
           !UIAccessibility.isReduceTransparencyEnabled {
            let effect = UIGlassEffect(style: .regular)
            effect.isInteractive = true
            effect.tintColor = color.withAlphaComponent(glassTintAlpha(for: opacity))
            let effectView = UIVisualEffectView(effect: effect)
            view = effectView
            contentView = effectView.contentView
            usesNativeGlassInteraction = true
        } else {
            let fallback = UIView()
            fallback.backgroundColor = color.withAlphaComponent(opacity)
            view = fallback
            contentView = fallback
            usesNativeGlassInteraction = false
        }

        view.isUserInteractionEnabled = true
        view.layer.cornerCurve = .continuous
        view.layer.cornerRadius = CGFloat(theme.cornerRadius)
        view.layer.borderWidth = usesNativeGlassInteraction ? 0 : CGFloat(theme.borderWidth)
        view.layer.borderColor = theme.border
            .uiColor(for: traitCollection)
            .cgColor
        view.clipsToBounds = true
        insertSubview(view, at: 0)
        contentView.addSubview(interactionControl)
        surfaceView = view
    }

    private func glassTintAlpha(for opacity: CGFloat) -> CGFloat {
        // Glass already supplies translucency and contrast. Theme opacity only
        // adjusts a restrained tint so it does not turn the material into a fill.
        let clamped = min(max(opacity, 0), 1)
        let base: CGFloat = spec.role.isSpecial ? 0.12 : 0.06
        let range: CGFloat = spec.role.isSpecial ? 0.26 : 0.22
        return base + clamped * range
    }

    private func handleTouchDown() {
        setPressed(true, animated: true)
        onEvent?(KeyboardKeyEvent(key: spec, phase: .pressed))
    }

    private func handleTouchUpInside() {
        setPressed(false, animated: true)
        onEvent?(KeyboardKeyEvent(key: spec, phase: .released))
    }

    private func handleTouchCancelled() {
        setPressed(false, animated: true)
        onEvent?(KeyboardKeyEvent(key: spec, phase: .cancelled))
    }

    private func setPressed(_ pressed: Bool, animated: Bool) {
        guard !usesNativeGlassInteraction else { return }
        let updates = {
            self.transform = pressed
                ? CGAffineTransform(scaleX: self.theme.pressedScale, y: self.theme.pressedScale)
                : .identity
            self.surfaceView?.alpha = pressed
                ? min(1, self.normalSurfaceAlpha * self.theme.pressedOpacityMultiplier)
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
}
#endif
