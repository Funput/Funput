#if canImport(UIKit)
import KeyboardLayout
import ThemeSchema
import UIKit
@MainActor
public final class KeyboardSurfaceView: UIView {
    public var backgroundImage: UIImage? {
        didSet { applyPresentation() }
    }
    public var presentation: KeyboardPresentation {
        didSet { presentationDidChange(from: oldValue) }
    }

    public var onKeyEvent: ((KeyboardKeyEvent) -> Void)?
    public var onSystemInputModeEvent: ((UIView, UIEvent) -> Void)?

    let backdropView = KeyboardBackdropView()
    let toolbarView = KeyboardToolbarView()
    let contentHost = UIView()
    let keysHost = KeyboardKeysHostView()
    let touchOverlay = KeyboardTouchOverlayView()
    let previewView = KeyboardKeyPreviewView()
    lazy var interactionController = KeyboardSurfaceInteractionController(
        feedbackView: self,
        onEvent: { [weak self] event in self?.onKeyEvent?(event) },
        onPreview: { [weak self] key, frame in self?.updatePreview(key, sourceFrame: frame) },
        onHighlight: { [weak self] key, highlighted in
            self?.keyControls[key.id]?.setPressed(highlighted, presentation: self?.presentation)
        }
    )
    var keyControls: [String: KeyboardKeyControl] = [:]
    var geometryCache: (
        size: CGSize,
        layout: KeyboardLayout,
        sizing: KeyboardSizingProfile,
        value: ResolvedKeyboard
    )?

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
        touchOverlay.frame = bounds

        guard bounds.width > 0, bounds.height > 0 else { return }
        let geometry = resolvedGeometry()
        toolbarView.frame = geometry.toolbarFrame ?? .zero
        geometry.keys.forEach { key in
            keyControls[key.spec.id]?.frame = key.frame
        }
        touchOverlay.updateGeometry(geometry)
    }

    public override func traitCollectionDidChange(_ previousTraits: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraits)
        guard previousTraits?.userInterfaceStyle != traitCollection.userInterfaceStyle else { return }
        applyPresentation()
    }

    public override func didMoveToWindow() {
        super.didMoveToWindow()
        if window == nil {
            touchOverlay.cancelAllTrackedTouches()
            interactionController.cancelAll()
        }
    }

    private func configureView() {
        isMultipleTouchEnabled = true
        contentHost.isMultipleTouchEnabled = true
        clipsToBounds = true
        isOpaque = false
        backgroundColor = .clear
        tintColor = .clear
        addSubview(backdropView)
        addSubview(contentHost)
        contentHost.addSubview(keysHost)
        contentHost.addSubview(toolbarView)
        contentHost.addSubview(touchOverlay)
        addSubview(previewView)
        configureTouchOverlay()
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

}
#endif
