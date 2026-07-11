#if canImport(UIKit)
import KeyboardLayout
import ThemeSchema
import UIKit

@MainActor
public final class KeyboardSurfaceView: UIView {
    public var presentation: KeyboardPresentation {
        didSet { presentationDidChange(from: oldValue) }
    }

    public var onKeyEvent: ((KeyboardKeyEvent) -> Void)?

    private let backdropView = UIVisualEffectView()
    private let gradientLayer = CAGradientLayer()
    private let toolbarView = KeyboardToolbarView()
    private let contentHost = UIView()
    private var keysHost = UIView()
    private var glassContainerView: UIVisualEffectView?
    private var usesGlassContainer = false
    private var keyControls: [String: KeyboardKeyControl] = [:]

    public init(presentation: KeyboardPresentation = KeyboardPresentation()) {
        self.presentation = presentation
        super.init(frame: .zero)
        commonInit()
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    public override var intrinsicContentSize: CGSize {
        CGSize(
            width: UIView.noIntrinsicMetric,
            height: KeyboardMetrics.recommendedHeight(
                for: traitCollection,
                scale: presentation.sizing.heightScale
            )
        )
    }

    public override func layoutSubviews() {
        super.layoutSubviews()
        backdropView.frame = bounds
        gradientLayer.frame = bounds
        contentHost.frame = bounds
        glassContainerView?.frame = bounds
        if glassContainerView == nil {
            keysHost.frame = bounds
        }

        guard bounds.width > 0, bounds.height > 0 else { return }
        let geometry = KeyboardGeometry.resolve(
            layout: presentation.layout,
            size: bounds.size,
            sizing: presentation.sizing,
            showsInputModeKey: presentation.showsInputModeKey
        )
        toolbarView.frame = geometry.toolbarFrame
        for key in geometry.keys {
            keyControls[key.spec.id]?.frame = key.frame
        }
    }

    public override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        guard previousTraitCollection?.userInterfaceStyle != traitCollection.userInterfaceStyle else { return }
        applyTheme()
    }

    private func commonInit() {
        clipsToBounds = true
        backgroundColor = .clear
        backdropView.isUserInteractionEnabled = false
        addSubview(backdropView)
        backdropView.contentView.layer.addSublayer(gradientLayer)
        addSubview(contentHost)
        contentHost.addSubview(toolbarView)
        toolbarView.onEvent = { [weak self] event in self?.onKeyEvent?(event) }
        ensureKeysHost()
        rebuildKeys()
        applyTheme()

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(accessibilityAppearanceDidChange),
            name: UIAccessibility.reduceTransparencyStatusDidChangeNotification,
            object: nil
        )
    }

    @objc private func accessibilityAppearanceDidChange() {
        applyTheme()
        setNeedsLayout()
    }

    private func presentationDidChange(from oldValue: KeyboardPresentation) {
        if oldValue.layout != presentation.layout ||
            oldValue.showsInputModeKey != presentation.showsInputModeKey {
            rebuildKeys()
        }
        applyTheme()
        invalidateIntrinsicContentSize()
        setNeedsLayout()
    }

    private func rebuildKeys() {
        keyControls.values.forEach { $0.removeFromSuperview() }
        keyControls.removeAll(keepingCapacity: true)

        let specs = presentation.layout.rows
            .flatMap(\.keys)
            .filter { presentation.showsInputModeKey || $0.role != .inputMode }
        for spec in specs {
            let control = KeyboardKeyControl(spec: spec)
            control.onEvent = { [weak self] event in self?.handle(event) }
            keyControls[spec.id] = control
            keysHost.addSubview(control)
        }
    }

    private func applyTheme() {
        let theme = presentation.theme
        ensureKeysHost()
        let reducesTransparency = UIAccessibility.isReduceTransparencyEnabled
        backdropView.effect = reducesTransparency
            ? nil
            : UIBlurEffect(style: .systemChromeMaterial)

        let startColor = theme.backgroundStart.uiColor(for: traitCollection)
        let endColor = theme.backgroundEnd.uiColor(for: traitCollection)
        gradientLayer.colors = [
            (reducesTransparency ? startColor.withAlphaComponent(1) : startColor).cgColor,
            (reducesTransparency ? endColor.withAlphaComponent(1) : endColor).cgColor,
        ]
        gradientLayer.locations = [0, 1]
        gradientLayer.startPoint = CGPoint(x: 0.08, y: 0)
        gradientLayer.endPoint = CGPoint(x: 0.92, y: 1)
        toolbarView.apply(theme: theme, traits: traitCollection)
        keyControls.values.forEach {
            $0.apply(theme: theme, shiftState: presentation.shiftState, traits: traitCollection)
        }
    }

    private func ensureKeysHost() {
        let shouldUseGlass = shouldUseGlassContainer
        guard keysHost.superview == nil || shouldUseGlass != usesGlassContainer else {
            updateGlassContainerSpacing()
            return
        }

        let controls = Array(keyControls.values)
        glassContainerView?.removeFromSuperview()
        if glassContainerView == nil {
            keysHost.removeFromSuperview()
        }

        if shouldUseGlass, #available(iOS 26.0, *) {
            let effect = UIGlassContainerEffect()
            effect.spacing = glassContainerSpacing
            let effectView = UIVisualEffectView(effect: effect)
            contentHost.insertSubview(effectView, belowSubview: toolbarView)
            glassContainerView = effectView
            keysHost = effectView.contentView
        } else {
            let host = UIView()
            contentHost.insertSubview(host, belowSubview: toolbarView)
            glassContainerView = nil
            keysHost = host
        }

        usesGlassContainer = shouldUseGlass
        controls.forEach(keysHost.addSubview)
        setNeedsLayout()
    }

    private var shouldUseGlassContainer: Bool {
        guard presentation.theme.material == .glass,
              !UIAccessibility.isReduceTransparencyEnabled else { return false }
        if #available(iOS 26.0, *) { return true }
        return false
    }

    private var glassContainerSpacing: CGFloat {
        let nearestGap = min(
            presentation.sizing.horizontalGap,
            presentation.sizing.verticalGap
        )
        return max(0, nearestGap - 1)
    }

    private func updateGlassContainerSpacing() {
        if #available(iOS 26.0, *),
           let effect = glassContainerView?.effect as? UIGlassContainerEffect {
            effect.spacing = glassContainerSpacing
        }
    }

    private func handle(_ event: KeyboardKeyEvent) {
        if event.key.role == .shift, event.phase == .released {
            var updated = presentation
            updated.shiftState = presentation.shiftState == .lowercase ? .uppercase : .lowercase
            presentation = updated
        }
        onKeyEvent?(event)
    }
}
#endif
