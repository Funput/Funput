#if canImport(UIKit)
import KeyboardLayout

extension KeyboardSurfaceInteractionController {
    /// Drops the presentation state a rebuilt layout invalidates, and nothing else.
    ///
    /// A finger still down when the layout swaps keeps its `ContactID`, its resolver entry
    /// and the geometry snapshot taken when it landed, so it still commits to the key the
    /// user actually touched. Only what is tied to the *old* keycap views goes: highlights,
    /// the preview, and a pending alternate palette whose anchor frame no longer exists.
    ///
    /// Deliberately not `cancelAll()`. That one is teardown for a surface on its way out,
    /// where an unfinished touch must not commit at all (architecture document §8.3). Using
    /// it here is what used to drop the press of every finger still on the keyboard.
    func suspendPresentation() {
        // A palette that has not opened yet would anchor to a frame that is about to be
        // rebuilt. One already open keeps its contact: the selection reads from
        // `initialKey.alternates`, not from geometry, so it still resolves on release.
        alternateHoldController.cancelAll()
        for (token, state) in touches {
            if let key = state.currentKey { setHighlighted(key, false) }
            touches[token]?.currentKey = nil
            touches[token]?.currentFrame = nil
        }
        refreshPreview()
    }
}
#endif
