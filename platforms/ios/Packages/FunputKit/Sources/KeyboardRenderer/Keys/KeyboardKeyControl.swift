#if canImport(UIKit)
import KeyboardLayout
import ThemeSchema
import UIKit

@MainActor
final class KeyboardKeyControl: UIControl {
    var onEvent: ((KeyboardKeyEvent) -> Void)?

    private let spec: KeySpec
    private let interactionControl = UIControl()
    private let contentView = KeyboardKeyContentView()
    private let surface = KeyboardKeySurfaceView()
    private var theme = KeyboardThemeTokens.funputGlass
    private var appliedSurfaceTheme: KeyboardThemeTokens?
    private var appliedInterfaceStyle: UIUserInterfaceStyle?
    private var appliedReduceTransparency: Bool?

    init(spec: KeySpec) {
        self.spec = spec
        super.init(frame: .zero)
        configureAccessibility()
        configureInteraction()
        interactionControl.addSubview(contentView)
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

        contentView.frame = bounds
        surface.updateShape(cornerRadius: theme.cornerRadius)
    }

    func apply(presentation: KeyboardPresentation, traits: UITraitCollection) {
        theme = presentation.theme
        applySurfaceIfNeeded(traits: traits)
        contentView.apply(spec: spec, presentation: presentation, traits: traits)
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
        if spec.role == .placeholder {
            isHidden = true
            isUserInteractionEnabled = false
            isAccessibilityElement = false
        }
    }

    private func configureInteraction() {
        contentView.isUserInteractionEnabled = false
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

    private func applySurfaceIfNeeded(traits: UITraitCollection) {
        let reduceTransparency = UIAccessibility.isReduceTransparencyEnabled
        guard appliedSurfaceTheme != theme
                || appliedInterfaceStyle != traits.userInterfaceStyle
                || appliedReduceTransparency != reduceTransparency else { return }
        surface.apply(theme: theme, spec: spec, traits: traits, content: interactionControl)
        appliedSurfaceTheme = theme
        appliedInterfaceStyle = traits.userInterfaceStyle
        appliedReduceTransparency = reduceTransparency
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
