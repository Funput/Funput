#if canImport(UIKit)
import UIKit

/// What a keyboard controller has to do to hold the process's surface.
@MainActor
public protocol KeyboardSurfaceClaimant: AnyObject {
    /// The view the surface fills while this claimant holds it.
    var surfaceContainer: UIView { get }

    /// The surface now belongs to this claimant. Every callback on it still points at
    /// whoever held it before, or nowhere: point them here and push this claimant's
    /// state.
    func bind(_ surface: KeyboardSurfaceView)

    /// Another claimant took the surface. Stop whatever would still write to it; the
    /// lifecycle callback that normally ends a presentation may arrive late or never.
    func surrenderSurface()
}
#endif
