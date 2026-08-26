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
            surface.onKeyEvent = { [weak self] event in
                self?.handleKeyEvent(event)
            }
            surface.onOverlayPadChanged = { [weak self] pad in
                guard let self else { return }
                UIView.performWithoutAnimation {
                    self.setPreferredHeightOverlayPad(pad)
                }
            }
            installClipboard(on: surface)
            installPersonalSuggestions(on: surface)
            view.addSubview(surface)
            NSLayoutConstraint.activate([
                surface.leadingAnchor.constraint(equalTo: view.leadingAnchor),
                surface.trailingAnchor.constraint(equalTo: view.trailingAnchor),
                surface.topAnchor.constraint(equalTo: view.topAnchor),
                surface.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            ])
            hasPrimarySurface = true
            launchTrace.recordPrimarySurfaceCreated()
            return surface
        }
    }
}
