#if canImport(UIKit)
import UIKit

/// Holds the one keyboard surface a process builds, and who is showing it.
///
/// The surface is the heaviest thing the keyboard makes: a view per key, each with
/// its own material. iOS creates a controller per host app inside the same process,
/// and building a surface for each made every app switch as costly as a cold start,
/// with two surfaces alive whenever the old controller outlasted the new one's load.
/// Here the surface is built once and moves from controller to controller.
@MainActor
public final class KeyboardSurfaceHost {
    public private(set) var surface: KeyboardSurfaceView?
    /// Weak, so a controller that dies leaves the surface free rather than held by
    /// nobody who could give it up.
    private weak var holder: (any KeyboardSurfaceClaimant)?
    /// Advances each time the surface changes hands. Whatever belongs to one host
    /// app's typing is keyed on it.
    public private(set) var session = 0

    public init() {}

    public var hasHolder: Bool { holder != nil }

    public func isHeld(by claimant: any KeyboardSurfaceClaimant) -> Bool {
        holder === claimant
    }

    /// Gives the surface to `claimant` if ``KeyboardSurfaceClaimPolicy`` allows it,
    /// building it with `make` the first time. Returns whether the claimant holds it.
    @discardableResult
    public func claim(
        for claimant: any KeyboardSurfaceClaimant,
        at moment: KeyboardSurfaceClaimPolicy.Moment,
        make: () -> KeyboardSurfaceView
    ) -> Bool {
        let decision = KeyboardSurfaceClaimPolicy.decide(
            at: moment,
            claimantHolds: isHeld(by: claimant),
            hasHolder: hasHolder,
            isAttached: surface?.superview != nil
        )
        switch decision {
        case .wait:
            return false
        case .keep:
            if let surface { pin(surface, into: claimant.surfaceContainer) }
            return true
        case .take:
            let previous = holder
            holder = claimant
            session += 1
            previous?.surrenderSurface()
            let surface = reusableSurface() ?? make()
            self.surface = surface
            pin(surface, into: claimant.surfaceContainer)
            claimant.bind(surface)
            return true
        }
    }

    /// Takes the surface out of the holder's view while the keyboard is off screen.
    ///
    /// Inside an input view, the surface and the input view keep each other alive
    /// through UIKit's own references, so neither goes when the controller does.
    /// Out of the hierarchy the input view is free to go, and the surface waits here
    /// for the next claim. A controller that no longer holds it is ignored.
    public func detach(by claimant: any KeyboardSurfaceClaimant) {
        guard isHeld(by: claimant) else { return }
        surface?.removeFromSuperview()
    }

    private func reusableSurface() -> KeyboardSurfaceView? {
        surface?.resetForReuse()
        return surface
    }

    private func pin(_ surface: KeyboardSurfaceView, into container: UIView) {
        guard surface.superview !== container else { return }
        // Leaving the old container takes the old constraints with it.
        surface.removeFromSuperview()
        surface.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(surface)
        NSLayoutConstraint.activate([
            surface.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            surface.trailingAnchor.constraint(equalTo: container.trailingAnchor),
            surface.topAnchor.constraint(equalTo: container.topAnchor),
            surface.bottomAnchor.constraint(equalTo: container.bottomAnchor),
        ])
    }
}
#endif
