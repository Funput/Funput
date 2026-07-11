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

    private let backdropView = KeyboardBackdropView()
    private let toolbarView = KeyboardToolbarView()
    private let contentHost = UIView()
    private let keysHost = KeyboardKeysHostView()
    private var keyControls: [String: KeyboardKeyControl] = [:]

    public init(presentation: KeyboardPresentation = KeyboardPresentation()) {
        self.presentation = presentation
        super.init(frame: .zero)
        configureView()
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
        contentHost.frame = bounds
        keysHost.frame = bounds

        guard bounds.width > 0, bounds.height > 0 else { return }
        let geometry = KeyboardGeometry.resolve(
            layout: presentation.layout,
            size: bounds.size,
            sizing: presentation.sizing,
            showsInputModeKey: presentation.showsInputModeKey
        )
        toolbarView.frame = geometry.toolbarFrame
        geometry.keys.forEach { key in
            keyControls[key.spec.id]?.frame = key.frame
        }
    }

    public override func traitCollectionDidChange(_ previousTraits: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraits)
        guard previousTraits?.userInterfaceStyle != traitCollection.userInterfaceStyle else { return }
        applyPresentation()
    }

    private func configureView() {
        clipsToBounds = true
        backgroundColor = .clear
        addSubview(backdropView)
        addSubview(contentHost)
        contentHost.addSubview(keysHost)
        contentHost.addSubview(toolbarView)
        toolbarView.onEvent = { [weak self] event in self?.onKeyEvent?(event) }
        rebuildKeys()
        applyPresentation()

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(accessibilityAppearanceDidChange),
            name: UIAccessibility.reduceTransparencyStatusDidChangeNotification,
            object: nil
        )
    }

    @objc private func accessibilityAppearanceDidChange() {
        applyPresentation()
        setNeedsLayout()
    }

    private func presentationDidChange(from oldValue: KeyboardPresentation) {
        if oldValue.layout != presentation.layout ||
            oldValue.showsInputModeKey != presentation.showsInputModeKey {
            rebuildKeys()
        }
        applyPresentation()
        invalidateIntrinsicContentSize()
        setNeedsLayout()
    }

    private func rebuildKeys() {
        keyControls.values.forEach { $0.removeFromSuperview() }
        let specs = presentation.layout.rows
            .flatMap(\.keys)
            .filter { presentation.showsInputModeKey || $0.role != .inputMode }
        keyControls = Dictionary(uniqueKeysWithValues: specs.map { spec in
            let control = KeyboardKeyControl(spec: spec)
            control.onEvent = { [weak self] event in self?.handle(event) }
            return (spec.id, control)
        })
        keysHost.install(Array(keyControls.values))
    }

    private func applyPresentation() {
        backdropView.apply(theme: presentation.theme, traits: traitCollection)
        keysHost.apply(presentation: presentation)
        toolbarView.apply(theme: presentation.theme, traits: traitCollection)
        keyControls.values.forEach {
            $0.apply(
                theme: presentation.theme,
                shiftState: presentation.shiftState,
                traits: traitCollection
            )
        }
    }

    private func handle(_ event: KeyboardKeyEvent) {
        if event.key.role == .shift, event.phase == .released {
            presentation.shiftState = presentation.shiftState == .lowercase
                ? .uppercase
                : .lowercase
        }
        onKeyEvent?(event)
    }
}
#endif
