import KeyboardRenderer
import UIKit

extension KeyboardViewController {
    /// Builds the process's surface from this controller's presentation.
    ///
    /// Runs once per process, from the first claim. `applyAdoptedPresentation` works
    /// out `currentPresentation` on the line before it claims, so creation always
    /// sees the final presentation. Callbacks are not wired here: `bind(_:)` does
    /// that for every controller that comes to hold the surface, this one included.
    func makePrimarySurface() -> KeyboardSurfaceView {
        launchTrace.measure("PrimarySurfaceCreation") {
            let surface = KeyboardSurfaceView(
                presentation: currentPresentation,
                backgroundImage: cachedBackgroundImage
            )
#if DEBUG
            surface.accessibilityIdentifier = "funput.keyboard.surface"
#endif
            KeyboardLifetimeCensus.track(surface, as: .surface)
            launchTrace.recordPrimarySurfaceCreated()
            return surface
        }
    }

    /// Takes the surface out of the input view while the keyboard is off screen.
    ///
    /// Inside the input view, the surface and the input view keep each other alive
    /// through UIKit's own references (the Auto Layout engine and its observation
    /// tracking), so neither goes when the controller does. Measured on a simulator:
    /// every app switch left a whole surface behind, about 2.5 MB that never came
    /// back. Out of the hierarchy the input view goes with its controller, and the
    /// surface waits in the runtime for whoever shows the keyboard next. The same
    /// controller can be shown again without a new `viewDidLoad`; its next claim
    /// puts the surface back.
    func detachPrimarySurface() {
        runtime.surfaceHost.detach(by: self)
    }
}
