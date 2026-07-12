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
    public var onSystemInputModeEvent: ((UIView, UIEvent) -> Void)?

    private let backdropView = KeyboardBackdropView()
    private let toolbarView = KeyboardToolbarView()
    private let contentHost = UIView()
    private let keysHost = KeyboardKeysHostView()
    let previewView = KeyboardKeyPreviewView()
    lazy var interactionController = KeyboardSurfaceInteractionController(
        onEvent: { [weak self] event in self?.onKeyEvent?(event) },
        onPreview: { [weak self] key, frame in self?.updatePreview(key, sourceFrame: frame) }
    )
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
                for: presentation.layout,
                traits: traitCollection,
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
            sizing: presentation.sizing
        )
        toolbarView.frame = geometry.toolbarFrame ?? .zero
        geometry.keys.forEach { key in
            keyControls[key.spec.id]?.frame = key.frame
        }
    }

    public override func traitCollectionDidChange(_ previousTraits: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraits)
        guard previousTraits?.userInterfaceStyle != traitCollection.userInterfaceStyle else { return }
        applyPresentation()
    }

    public override func didMoveToWindow() {
        super.didMoveToWindow()
        if window == nil {
            interactionController.cancelAll()
        }
    }

    private func configureView() {
        clipsToBounds = true
        isOpaque = false
        backgroundColor = .clear
        // A keyboard extension can inherit the host application's tint. Glass
        // surfaces define their own semantic tint and must not inherit that value.
        tintColor = .clear
        addSubview(backdropView)
        addSubview(contentHost)
        contentHost.addSubview(keysHost)
        contentHost.addSubview(toolbarView)
        addSubview(previewView)
        toolbarView.onEvent = { [weak self] event in self?.route(event, from: nil) }
        toolbarView.onSystemInputModeEvent = { [weak self] source, event in
            self?.onSystemInputModeEvent?(source, event)
        }
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
        if oldValue.layout != presentation.layout {
            interactionController.cancelAll()
            rebuildKeys()
        }
        applyPresentation()
        invalidateIntrinsicContentSize()
        setNeedsLayout()
    }

    private func rebuildKeys() {
        keyControls.values.forEach { $0.removeFromSuperview() }
        let specs = presentation.layout.rows.flatMap(\.keys)
        keyControls = Dictionary(uniqueKeysWithValues: specs.map { spec in
            let control = KeyboardKeyControl(spec: spec)
            control.onEvent = { [weak self, weak control] event in
                self?.route(event, from: control)
            }
            return (spec.id, control)
        })
        keysHost.install(Array(keyControls.values))
    }

    private func applyPresentation() {
        backdropView.apply(theme: presentation.theme, traits: traitCollection)
        keysHost.apply(presentation: presentation)
        toolbarView.apply(
            spec: presentation.layout.toolbar,
            theme: presentation.theme,
            traits: traitCollection
        )
        keyControls.values.forEach {
            $0.apply(presentation: presentation, traits: traitCollection)
        }
    }

}
#endif
