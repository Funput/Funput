#if canImport(UIKit)
/// Decides whether a keyboard controller may take the process's one surface.
///
/// iOS builds a new controller for every host app and may load it while the previous
/// one is still on screen. A controller that is only loading must therefore not take
/// a surface someone is showing; a controller that is appearing is the keyboard the
/// user is about to see, and takes it whatever its state.
public enum KeyboardSurfaceClaimPolicy {
    public enum Moment: Sendable {
        /// `viewDidLoad`: the controller exists but may never be shown.
        case loading
        /// `viewWillAppear` or later: the controller is the visible keyboard.
        case appearing
    }

    public enum Decision: Equatable, Sendable {
        /// The claimant already holds the surface.
        case keep
        /// The surface passes to the claimant.
        case take
        /// Someone else is showing the surface; ask again when appearing.
        case wait
    }

    /// - Parameters:
    ///   - claimantHolds: the claimant is the current holder.
    ///   - hasHolder: a living controller holds the surface.
    ///   - isAttached: the surface sits in some controller's view.
    public static func decide(
        at moment: Moment,
        claimantHolds: Bool,
        hasHolder: Bool,
        isAttached: Bool
    ) -> Decision {
        if claimantHolds { return .keep }
        switch moment {
        case .appearing: return .take
        case .loading: return hasHolder && isAttached ? .wait : .take
        }
    }
}
#endif
