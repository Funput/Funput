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

    private let gradientLayer = CAGradientLayer()
    private let toolbarView = KeyboardToolbarView()
    private let contentHost = UIView()
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
        gradientLayer.frame = bounds
        contentHost.frame = bounds

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
        layer.addSublayer(gradientLayer)
        addSubview(contentHost)
        contentHost.addSubview(toolbarView)
        toolbarView.onEvent = { [weak self] event in self?.onKeyEvent?(event) }
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
            contentHost.addSubview(control)
        }
    }

    private func applyTheme() {
        let theme = presentation.theme
        gradientLayer.colors = [
            theme.backgroundStart.uiColor(for: traitCollection).cgColor,
            theme.backgroundEnd.uiColor(for: traitCollection).cgColor,
        ]
        gradientLayer.startPoint = CGPoint(x: 0, y: 0)
        gradientLayer.endPoint = CGPoint(x: 1, y: 1)
        toolbarView.apply(theme: theme, traits: traitCollection)
        keyControls.values.forEach {
            $0.apply(theme: theme, shiftState: presentation.shiftState, traits: traitCollection)
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
