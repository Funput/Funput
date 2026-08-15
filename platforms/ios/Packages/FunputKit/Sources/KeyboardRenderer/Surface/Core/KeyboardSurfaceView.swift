#if canImport(UIKit)
import KeyboardLayout
import ThemeSchema
import UIKit
@MainActor
public final class KeyboardSurfaceView: UIView {
    var backgroundImageState: UIImage?
    var presentationState: KeyboardPresentation
#if DEBUG
    var keyRebuildCount = 0
    var presentationApplyCount = 0
#endif
    public var backgroundImage: UIImage? {
        get { backgroundImageState }
        set {
            guard backgroundImageState !== newValue else { return }
            backgroundImageState = newValue
            applyBackdropPresentation()
        }
    }
    public var presentation: KeyboardPresentation {
        get { presentationState }
        set {
            let oldValue = presentationState
            presentationState = newValue
            presentationDidChange(from: oldValue)
        }
    }
    public var onKeyEvent: ((KeyboardKeyEvent) -> Void)?
    public var onSuggestionSelected: ((KeyboardSuggestionCandidate) -> Void)?
    public var onClipboardPaste: ((String) -> Void)?

    let backdropView = KeyboardBackdropView()
    let toolbarView = KeyboardToolbarView()
    let contentHost = UIView()
    let keysHost = KeyboardKeysHostView()
    let touchOverlay = KeyboardTouchOverlayView()
    let previewView = KeyboardKeyPreviewView()
    let alternatePaletteView = KeyboardAlternatePaletteView()
    lazy var touchCoordinator = KeyboardTouchCoordinator {
        [weak self] event in self?.emitTouchEvent(event)
    }
    lazy var interactionController = KeyboardSurfaceInteractionController(
        feedbackView: self,
        onEvent: { [weak self] event in self?.emitTouchEvent(event) },
        onContactEvent: { [weak self] token, event in
            self?.handleContactInteractionEvent(token: token, event: event)
        },
        onClaimGesture: { [weak self] token, kind in
            self?.claimContactGesture(token, kind: kind) ?? false
        },
        onPreview: { [weak self] key, frame in self?.updatePreview(key, sourceFrame: frame) },
        onAlternatePreview: { [weak self] key, layout, selectedIndex in
            self?.updateAlternates(key, layout: layout, selectedIndex: selectedIndex)
        },
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
    public init(
        presentation: KeyboardPresentation = KeyboardPresentation(),
        backgroundImage: UIImage? = nil
    ) {
        presentationState = presentation
        backgroundImageState = backgroundImage
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
        // The coordinator owns the snapshot and its revision; the overlay borrows the same one.
        touchCoordinator.updateGeometry(geometry)
        touchOverlay.adoptGeometry(touchCoordinator.geometrySnapshot)
    }

    public override func didMoveToWindow() {
        super.didMoveToWindow()
        if window == nil {
            touchOverlay.forgetTrackedTouches()
            interactionController.cancelAll()
            resetTouchPipeline()
        }
    }

}
#endif
