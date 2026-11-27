import Foundation
import KeyboardRenderer
import UIKit

/// A handle a screen keeps to photograph the keyboard preview it shows. The preview
/// registers its surface here; the handle never keeps one alive.
@MainActor
final class KeyboardPreviewSnapshotter {
    /// Pixel width of a capture: the card's 316pt thumbnail slot at 3x, the density
    /// the bundled artwork is exported at.
    static let pixelWidth: CGFloat = 948
    /// How long a preview must have shown the same thing before it is worth a picture.
    /// Glass composites in the render server a few frames after a change.
    static let settleDelay: TimeInterval = 0.8

    weak var surface: KeyboardPreviewSurface?

    /// Whether the preview can stand in for the real keyboard right now: on screen,
    /// settled, untouched since its last change (a touched one may show a key popup),
    /// and at its natural height rather than squeezed onto a short screen.
    func isReady(now: Date = Date()) -> Bool {
        guard let surface, surface.window != nil, !surface.wasTouched,
              now.timeIntervalSince(surface.changedAt) >= Self.settleDelay
        else { return false }
        let presentation = surface.surface.presentation
        let natural = KeyboardMetrics.phonePortraitHeight(
            for: presentation.layout,
            sizing: presentation.previewSizing
        )
        return surface.bounds.height >= natural - 1
    }

    /// The preview as drawn on screen, or `nil` when it is not ready for a picture.
    func capture() -> UIImage? {
        guard isReady(), let surface else { return nil }
        let format = UIGraphicsImageRendererFormat()
        format.scale = Self.pixelWidth / surface.bounds.width
        format.opaque = true
        return UIGraphicsImageRenderer(bounds: surface.bounds, format: format).image { _ in
            surface.drawHierarchy(in: surface.bounds, afterScreenUpdates: true)
        }
    }
}
