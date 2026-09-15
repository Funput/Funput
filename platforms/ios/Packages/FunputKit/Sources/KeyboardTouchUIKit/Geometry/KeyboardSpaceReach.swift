import CoreGraphics
import KeyboardLayout

/// The strip above the spacebar that the spacebar answers for, past its own top edge.
///
/// Row gaps are otherwise split at their midpoint, which leaves the spacebar 3.5pt of slack
/// above it on the default profile — a thumb aiming for it from the letter row lands on `n`
/// or `m` instead, and a thumb that types without looking never learns why. The gap is not
/// symmetric in practice either: a thumb rolls onto the spacebar from above, so its contact
/// centroid sits high of where the user believes they pressed, and the error only ever points
/// one way.
///
/// The claim therefore takes the whole gap plus a few points of the row above, rather than
/// moving the shared midpoint — the keys above keep their slack downward into the gap they
/// already had. `n` gives up the bottom tenth of its keycap for this, which is the cost of
/// the trade: aiming at the very bottom edge of `n` now produces a space. Aiming at `n` at
/// all — anywhere in the remaining nine tenths — does not.
public enum KeyboardSpaceReach {
    /// How far into the row above the spacebar answers, measured from that row's bottom edge.
    public static let overlap: CGFloat = 4

    /// Ceiling on `overlap` as a share of the row above. Key height is a user setting, and at
    /// the smallest scale a fixed 4pt would swallow a quarter of the row rather than a tenth.
    public static let maximumNeighbourFraction: CGFloat = 0.1

    /// The region `key` claims from the row whose keycaps end at `neighbourBottom`, or nil
    /// when the key earns no reach. Only the spacebar does.
    ///
    /// The rect spans the spacebar's own width, so keys sitting beyond either end of it —
    /// `m` on a phone-width layout — keep their full keycap.
    public static func claim(
        for key: ResolvedKey,
        neighbourBottom: CGFloat,
        neighbourHeight: CGFloat
    ) -> CGRect? {
        guard key.spec.role == .space, neighbourBottom < key.frame.minY else { return nil }
        let top = neighbourBottom - min(overlap, neighbourHeight * maximumNeighbourFraction)
        return CGRect(
            x: key.frame.minX,
            y: top,
            width: key.frame.width,
            height: key.frame.minY - top
        )
    }
}
