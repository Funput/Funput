import Foundation
import FunputShared
import KeyboardRenderer
import Testing
import ThemeRuntime
import ThemeSchema
import UIKit
@testable import Funput

/// The dock photographs its preview only when the picture can stand in for the real
/// keyboard: on screen, settled, untouched since its last change, and full height.
@MainActor
struct KeyboardPreviewSnapshotterTests {
    @Test("A preview is ready once it has settled on screen at full height")
    func readyWhenSettled() {
        let (snapshotter, surface, window) = mountedPreview()

        #expect(!snapshotter.isReady(now: surface.changedAt))
        #expect(snapshotter.isReady(now: settled(surface)))
        surface.removeFromSuperview()
        #expect(!snapshotter.isReady(now: settled(surface)))
        _ = window
    }

    @Test("A touch blocks the picture until the preview shows something new")
    func touchResetsOnNewContent() {
        let (snapshotter, surface, window) = mountedPreview()

        surface.wasTouched = true
        #expect(!snapshotter.isReady(now: settled(surface)))
        surface.apply(presentation: presentation(.default), backgroundImageData: nil, isInteractive: true)
        #expect(surface.wasTouched)

        let touchedAt = surface.changedAt
        var midnight = FunputConfiguration.default
        midnight.selectedThemeID = KeyboardTheme.midnight.id
        surface.apply(presentation: presentation(midnight), backgroundImageData: nil, isInteractive: true)
        #expect(!surface.wasTouched)
        #expect(surface.changedAt > touchedAt)
        _ = window
    }

    @Test("A preview squeezed onto a short screen is not photographed")
    func squeezedPreviewIsSkipped() {
        let (snapshotter, surface, window) = mountedPreview()

        surface.frame.size.height -= 40
        #expect(!snapshotter.isReady(now: settled(surface)))
        _ = window
    }

    private func mountedPreview() -> (KeyboardPreviewSnapshotter, KeyboardPreviewSurface, UIWindow) {
        let presentation = presentation(.default)
        let height = KeyboardMetrics.phonePortraitHeight(
            for: presentation.layout,
            sizing: presentation.previewSizing
        )
        let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 402, height: 874))
        let surface = KeyboardPreviewSurface(frame: CGRect(x: 0, y: 0, width: 402, height: height))
        surface.apply(presentation: presentation, backgroundImageData: nil, isInteractive: true)
        window.addSubview(surface)
        let snapshotter = KeyboardPreviewSnapshotter()
        snapshotter.surface = surface
        return (snapshotter, surface, window)
    }

    /// Just past the moment the surface counts as settled after its latest change (the
    /// margin keeps `Date` rounding from landing a hair before the delay).
    private func settled(_ surface: KeyboardPreviewSurface) -> Date {
        surface.changedAt.addingTimeInterval(KeyboardPreviewSnapshotter.settleDelay + 0.01)
    }

    private func presentation(_ configuration: FunputConfiguration) -> KeyboardPresentation {
        KeyboardPreviewPresentation.make(configuration: configuration)
    }
}
