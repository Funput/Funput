#if canImport(UIKit)
import CoreGraphics
import KeyboardLayout

/// Follows a finger pulling down on an iPad key to type the character printed above it.
///
/// The stock iPad keyboard types the hint (`1` over `q`, `@` over `a`) when a key is pulled
/// down instead of tapped; the letter fades and the hint slides down with the finger. Only
/// keys printed that way qualify: the iPad's bottom-centered keys that carry a hint. The
/// phone never places a label there, so it never gets the gesture.
struct KeySwipeDownTracker {
    /// How far a finger must pull, as a share of the key's height, before release types
    /// the hint.
    static let travelShare: CGFloat = 0.45
    static let minimumTravel: CGFloat = 20
    /// Movement under this is still a tap, matching the interaction controller's slop.
    static let slop: CGFloat = 16
    /// Downward travel must outweigh sideways travel by this much.
    static let verticalDominance: CGFloat = 1.25
    /// Once claimed, a release past this share of the pull still types the hint.
    static let releaseShare: CGFloat = 0.5

    let travel: CGFloat
    private(set) var progress: CGFloat = 0
    /// Set once the pull reached the end and the gesture took the contact over.
    var claimed = false

    init?(key: KeySpec, keyHeight: CGFloat) {
        guard Self.isEligible(key) else { return nil }
        travel = max(Self.minimumTravel, keyHeight * Self.travelShare)
    }

    static func isEligible(_ key: KeySpec) -> Bool {
        key.labelPlacement == .bottomCenter && key.secondaryLabel?.isEmpty == false
    }

    /// The pull's progress from 0 to 1, or nil while the finger is not pulling down.
    /// A claimed pull keeps tracking whatever the direction, so the finger can back out.
    mutating func update(translation: CGPoint) -> CGFloat? {
        let down = translation.y
        let pulling = down > Self.slop && down > abs(translation.x) * Self.verticalDominance
        guard claimed || pulling else {
            progress = 0
            return nil
        }
        progress = min(1, max(0, down / travel))
        return progress
    }

    /// Whether a release now types the hint rather than the letter.
    var typesHint: Bool { claimed && progress >= Self.releaseShare }
}
#endif
