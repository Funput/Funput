#if canImport(UIKit)
import KeyboardLayout
import ThemeSchema
import UIKit
@MainActor
final class KeyboardKeyControl: UIControl {
    var onEvent: ((KeyboardKeyEvent) -> Void)?
    private(set) var spec: KeySpec
    let interactionControl = UIControl()
    let contentView = KeyboardKeyContentView()
    let surface = KeyboardKeySurfaceView()
    private(set) var theme = ResolvedTheme.funputGlass
    private var appliedSurfaceTheme: ResolvedTheme?
    private var appliedInterfaceStyle: UIUserInterfaceStyle?
    private var appliedReduceTransparency: Bool?
    var swipeTracker = KeySwipeGestureTracker()
    var swipeRecognizer: UIPanGestureRecognizer?
    private var visualFrame = CGRect.zero
    var role: KeyRole { spec.role }

    init(spec: KeySpec) {
        self.spec = spec
        super.init(frame: .zero)
        accessibilityTraits = .keyboardKey
        clipsToBounds = false
        configureInteraction()
        applySpec()
        insertSubview(surface, at: 0)
        addSubview(interactionControl)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    /// Becomes another key. A layout change hands the controls it has to the new
    /// layout rather than building new ones, because the keycap underneath is the
    /// costly part: under a glass theme it is a `UIVisualEffectView` per key. The
    /// next `apply(presentation:traits:)` draws the new key's content.
    func adopt(_ spec: KeySpec) {
        // The keycap's colours follow whether the key is special, and nothing else
        // about the spec; only that change needs the surface built again.
        if spec.role.isSpecial != self.spec.role.isSpecial { appliedSurfaceTheme = nil }
        self.spec = spec
        applySpec()
        surface.setPressed(false, theme: theme, animated: false)
        contentView.setSwipeProgress(0, animated: false)
    }

    /// Out of use until a layout needs it again. It stays in the hierarchy, so its
    /// keycap is not rebuilt when it comes back.
    func park() {
        isHidden = true
        isUserInteractionEnabled = false
        isAccessibilityElement = false
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        let visual = visualFrame.isEmpty ? bounds : visualFrame
        let keycapHeight = visual.height * theme.keycapHeightScale
        surface.frame = CGRect(
            x: visual.minX,
            y: visual.midY - keycapHeight / 2,
            width: visual.width,
            height: keycapHeight
        )
        interactionControl.frame = bounds
        contentView.frame = surface.bounds
        surface.updateShape(cornerRadius: theme.cornerRadius)
    }

    func applyFrames(interaction: CGRect, visual: CGRect) {
        frame = interaction
        visualFrame = visual.offsetBy(dx: -interaction.minX, dy: -interaction.minY)
        setNeedsLayout()
    }

    func apply(presentation: KeyboardPresentation, traits: UITraitCollection) {
        theme = presentation.theme
        applySurfaceIfNeeded(traits: traits)
        contentView.apply(spec: spec, presentation: presentation, traits: traits)
        accessibilityCustomActions = KeyboardKeyAccessibilityActions.make(
            spec: spec,
            presentation: presentation
        ) { [weak self] phase in
            self?.emit(phase)
        }
        setNeedsLayout()
    }

    override func accessibilityActivate() -> Bool {
        emit(.pressed)
        emit(.released)
        return true
    }

    func setPressed(_ pressed: Bool, presentation: KeyboardPresentation?) {
        guard let presentation else { return }
        surface.setPressed(pressed, theme: presentation.theme, animated: true)
    }

    func setSwipeProgress(_ progress: CGFloat, animated: Bool) {
        contentView.setSwipeProgress(progress, animated: animated)
    }

    private func applySurfaceIfNeeded(traits: UITraitCollection) {
        let reduceTransparency = UIAccessibility.isReduceTransparencyEnabled
        guard appliedSurfaceTheme != theme
                || appliedInterfaceStyle != traits.userInterfaceStyle
                || appliedReduceTransparency != reduceTransparency else { return }
        surface.apply(theme: theme, spec: spec, traits: traits, content: contentView)
        appliedSurfaceTheme = theme
        appliedInterfaceStyle = traits.userInterfaceStyle
        appliedReduceTransparency = reduceTransparency
    }
}
#endif
