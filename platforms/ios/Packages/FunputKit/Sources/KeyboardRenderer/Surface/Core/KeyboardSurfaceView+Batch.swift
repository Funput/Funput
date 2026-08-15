#if canImport(UIKit)
import UIKit

extension KeyboardSurfaceView {
    public func apply(
        presentation: KeyboardPresentation,
        backgroundImage: UIImage?
    ) {
        let previous = presentationState
        let backgroundChanged = backgroundImageState !== backgroundImage
        guard previous != presentation || backgroundChanged else { return }
        presentationState = presentation
        backgroundImageState = backgroundImage
        presentationDidChange(
            from: previous,
            backgroundChanged: backgroundChanged
        )
    }
}
#endif
