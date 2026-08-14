import KeyboardRenderer
import UIKit

extension KeyboardViewController {
    func applyPrimarySurface(
        presentation: KeyboardPresentation,
        backgroundImage: UIImage?
    ) -> KeyboardSurfaceView {
        if hasPrimarySurface {
            keyboardView.apply(
                presentation: presentation,
                backgroundImage: backgroundImage
            )
        } else {
            _ = keyboardView
        }
        return keyboardView
    }

    func makePrimarySurface() -> KeyboardSurfaceView {
        launchTrace.measure("PrimarySurfaceCreation") {
            let surface = KeyboardSurfaceView(
                presentation: currentPresentation,
                backgroundImage: cachedBackgroundImage
            )
            surface.translatesAutoresizingMaskIntoConstraints = false
            surface.onKeyEvent = { [weak self] event in
                self?.handleKeyEvent(event)
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
