import KeyboardRenderer
import UIKit

extension KeyboardViewController {
    /// Builds the primary surface from whatever `currentPresentation` holds.
    ///
    /// The only caller is the `keyboardView` lazy initializer, and the only thing that
    /// touches `keyboardView` first is `applyPresentationToSurfaces`, which assigns
    /// `currentPresentation` on the line before. Creation therefore always sees the
    /// final presentation — unlike the previous `applyPrimarySurface`, which took a
    /// presentation it then ignored on the creation path.
    func makePrimarySurface() -> KeyboardSurfaceView {
        launchTrace.measure("PrimarySurfaceCreation") {
            let surface = KeyboardSurfaceView(
                presentation: currentPresentation,
                backgroundImage: cachedBackgroundImage
            )
#if DEBUG
            surface.accessibilityIdentifier = "funput.keyboard.surface"
#endif
            surface.translatesAutoresizingMaskIntoConstraints = false
            KeyboardLifetimeCensus.track(surface, as: .surface)
            surface.onKeyEvent = { [weak self] event in
                self?.handleKeyEvent(event)
            }
            surface.setInputModeSwitch(
                target: self,
                action: #selector(handleInputModeList(from:with:))
            )
            installClipboard(on: surface)
            installPersonalSuggestions(on: surface)
            pin(surface)
            hasPrimarySurface = true
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
    /// back, until the extension would run into its memory limit. Out of the
    /// hierarchy, the surface is owned by this controller alone and goes with it, and
    /// the input view goes too.
    func detachPrimarySurface() {
        guard hasPrimarySurface, keyboardView.superview != nil else { return }
        keyboardView.removeFromSuperview()
    }

    /// The same controller can be shown again without a new `viewDidLoad`, so the
    /// surface goes back in before the presentation is worked out.
    func reattachPrimarySurfaceIfNeeded() {
        guard hasPrimarySurface, keyboardView.superview == nil else { return }
        pin(keyboardView)
    }

    private func pin(_ surface: KeyboardSurfaceView) {
        view.addSubview(surface)
        NSLayoutConstraint.activate([
            surface.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            surface.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            surface.topAnchor.constraint(equalTo: view.topAnchor),
            surface.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
    }
}
