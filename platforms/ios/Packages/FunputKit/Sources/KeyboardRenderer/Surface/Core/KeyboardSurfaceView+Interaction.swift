#if canImport(UIKit)
import KeyboardLayout
import UIKit

extension KeyboardSurfaceView {
    func route(_ event: KeyboardKeyEvent, from sourceView: UIView?) {
        handleV2Event(event)
    }

    func updatePreview(_ key: KeySpec?, sourceFrame: CGRect?) {
        guard let key, let sourceFrame else {
            previewView.hide()
            return
        }
        previewView.show(
            key: key,
            sourceFrame: sourceFrame,
            presentation: presentation,
            traits: traitCollection,
            containerBounds: bounds
        )
        bringSubviewToFront(previewView)
    }

    func updateAlternates(
        _ key: KeySpec?,
        layout: KeyboardAlternatePaletteLayout?,
        selectedIndex: Int?
    ) {
        guard let key, let layout else {
            alternatePaletteView.hide()
            return
        }
        alternatePaletteView.show(
            key: key,
            layout: layout,
            selectedIndex: selectedIndex,
            presentation: presentation,
            traits: traitCollection
        )
        bringSubviewToFront(alternatePaletteView)
    }
}
#endif
