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
    private let spacebarContent = KeyboardSpacebarContentView()
    private let surface = KeyboardKeySurfaceView()
    private var theme = KeyboardThemeTokens.funputGlass
    private var shiftState = ShiftState.lowercase

    init(spec: KeySpec) {
        self.spec = spec
        super.init(frame: .zero)
        configureAccessibility()
        configureInteraction()
        configureContent()
        insertSubview(surface, at: 0)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        surface.frame = bounds
        interactionControl.frame = bounds

        let contentInset = max(6, bounds.height * 0.2)
        label.frame = bounds.insetBy(dx: 5, dy: contentInset * 0.5)
        iconView.frame = bounds.insetBy(dx: contentInset, dy: contentInset)
        spacebarContent.frame = bounds
        surface.updateShape(cornerRadius: theme.cornerRadius)
    }

    func apply(theme: KeyboardThemeTokens, shiftState: ShiftState, traits: UITraitCollection) {
        self.theme = theme
        self.shiftState = shiftState
        surface.apply(theme: theme, spec: spec, traits: traits, content: interactionControl)

        let labelColor = theme.label.uiColor(for: traits)
        label.textColor = spec.role == .space
            ? theme.secondaryLabel.uiColor(for: traits)
            : labelColor
        iconView.tintColor = labelColor
        label.font = KeyboardKeyContentStyle.font(for: spec.role, scale: theme.fontScale)
        label.text = KeyboardKeyContentStyle.label(for: spec, shiftState: shiftState)
        iconView.image = KeyboardKeyContentStyle.icon(for: spec.role, shiftState: shiftState)
        spacebarContent.apply(
            label: spec.label,
            color: theme.secondaryLabel.uiColor(for: traits),
            font: label.font
        )
        let showsSpacebar = spec.role == .space
        spacebarContent.isHidden = !showsSpacebar
        label.isHidden = showsSpacebar || iconView.image != nil
        iconView.isHidden = showsSpacebar || iconView.image == nil
        setNeedsLayout()
    }

    override func accessibilityActivate() -> Bool {
        emit(.pressed)
        emit(.released)
        return true
    }

    private func configureAccessibility() {
        isAccessibilityElement = true
        accessibilityLabel = spec.accessibilityLabel
        accessibilityTraits = .keyboardKey
        clipsToBounds = false
        interactionControl.isAccessibilityElement = false
    }

    private func configureInteraction() {
        interactionControl.addAction(UIAction { [weak self] _ in
            self?.handleTouch(.pressed)
        }, for: .touchDown)
        interactionControl.addAction(UIAction { [weak self] _ in
            self?.handleTouch(.released)
        }, for: .touchUpInside)
        interactionControl.addAction(UIAction { [weak self] _ in
            self?.handleTouch(.cancelled)
        }, for: [.touchCancel, .touchUpOutside])
    }

    private func configureContent() {
        label.textAlignment = .center
        label.adjustsFontSizeToFitWidth = true
        label.minimumScaleFactor = 0.7
        label.isUserInteractionEnabled = false
        interactionControl.addSubview(label)

        iconView.contentMode = .scaleAspectFit
        iconView.preferredSymbolConfiguration = UIImage.SymbolConfiguration(weight: .medium)
        iconView.isUserInteractionEnabled = false
        interactionControl.addSubview(iconView)

        spacebarContent.isUserInteractionEnabled = false
        interactionControl.addSubview(spacebarContent)
    }

    private func handleTouch(_ phase: KeyboardKeyEvent.Phase) {
        surface.setPressed(phase == .pressed, theme: theme, animated: true)
        emit(phase)
    }

    private func emit(_ phase: KeyboardKeyEvent.Phase) {
        onEvent?(KeyboardKeyEvent(key: spec, phase: phase))
    }
}
#endif
