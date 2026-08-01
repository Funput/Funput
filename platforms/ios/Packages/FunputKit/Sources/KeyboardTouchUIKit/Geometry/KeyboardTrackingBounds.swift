import CoreGraphics
import KeyboardLayout

/// The region the keycap touch surface owns.
///
/// Single source for the calculation: the capture view and the geometry snapshot both hit-test
/// against it, and a disagreement between them would make a press highlight one key and commit
/// another.
public enum KeyboardTrackingBounds {
    /// Slack around the keycap union, because a fast tap often lands just outside a key.
    public static let outerTolerance: CGFloat = 12

    public static func resolve(for geometry: ResolvedKeyboard) -> CGRect {
        let union = geometry.keys.reduce(CGRect.null) { $0.union($1.frame) }
        guard !union.isNull else { return .null }
        let padded = union.insetBy(dx: -outerTolerance, dy: -outerTolerance)
        // The toolbar handles its own touches. Letting the keycap slack reach over it would
        // swallow the bottom edge of every suggestion.
        //
        // Clamp only when the toolbar genuinely sits above the keys. Any other arrangement
        // keeps the plain slack, so the region can never collapse into a sliver and leave the
        // keyboard unable to receive touches at all.
        guard let toolbar = geometry.toolbarFrame,
              toolbar.maxY <= union.minY,
              toolbar.maxY > padded.minY else { return padded }
        return CGRect(
            x: padded.minX,
            y: toolbar.maxY,
            width: padded.width,
            height: padded.maxY - toolbar.maxY
        )
    }
}
