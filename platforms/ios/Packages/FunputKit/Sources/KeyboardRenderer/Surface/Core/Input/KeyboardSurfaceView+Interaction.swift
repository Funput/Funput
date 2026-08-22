#if canImport(UIKit)
import KeyboardLayout
import UIKit

extension KeyboardSurfaceView {
    func route(_ event: KeyboardKeyEvent, from sourceView: UIView?) {
        emitTouchEvent(event)
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
            containerBounds: keyboardBounds
        )
        bringSubviewToFront(previewView)
    }

    func updateAlternates(
        _ key: KeySpec?,
        layout: KeyboardAlternatePaletteLayout?,
        selectedIndex: Int?
    ) {
        syncOverlayPad(layout?.overflowAbove ?? 0)
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
        alternatePaletteView.frame = layout.frame.offsetBy(dx: 0, dy: overlayPadTop)
        bringSubviewToFront(alternatePaletteView)
    }

    func syncOverlayPad(_ overflow: CGFloat) {
        let next = ceil(overflow)
        guard overlayPadTop != next else { return }
        overlayPadTop = next
        onOverlayPadChanged?(next)
        setNeedsLayout()
    }
}
#endif
