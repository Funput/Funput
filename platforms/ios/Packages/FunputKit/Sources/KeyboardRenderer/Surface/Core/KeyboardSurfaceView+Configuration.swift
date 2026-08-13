#if canImport(UIKit)
import UIKit

extension KeyboardSurfaceView {
    func configureView() {
        registerForTraitChanges([UITraitUserInterfaceStyle.self]) { (view: Self, _) in
            view.applyPresentation()
        }
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
        addSubview(alternatePaletteView)
        configureTouchOverlay()
        configureTouchPipeline()
        configureToolbarCallbacks()
        rebuildKeys()
        applyPresentation()

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(accessibilityAppearanceDidChange),
            name: UIAccessibility.reduceTransparencyStatusDidChangeNotification,
            object: nil
        )
    }

    private func configureToolbarCallbacks() {
        toolbarView.onEvent = { [weak self] event in
            self?.route(event, from: nil)
        }
        toolbarView.onSuggestionSelected = { [weak self] candidate in
            guard let self else { return }
            interactionController.performSuggestionFeedback(presentation: presentation)
            onSuggestionSelected?(candidate)
        }
        toolbarView.onClipboardPaste = { [weak self] text in
            guard let self else { return }
            interactionController.performSuggestionFeedback(presentation: presentation)
            onClipboardPaste?(text)
        }
    }
}
#endif
