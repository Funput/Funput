#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import UIKit

/// A surface that moves to another host app's keyboard starts there as a new one
/// would: nothing of the app it came from may show or act.
@MainActor
@Suite("Keyboard surface reuse")
struct KeyboardSurfaceReuseTests {
    @Test("A reset surface shows no suggestions and no paste offer, and is visible")
    func resetClearsWhatTheLastHostLeft() {
        let surface = makeSurface()
        surface.updateSuggestions([KeyboardSuggestionCandidate(text: "xin", generation: 1)])
        surface.updateClipboardHint(.text)
        surface.onSuggestionEdit = { _, _ in }
        surface.isHidden = true

        surface.resetForReuse()

        #expect(surface.toolbarView.suggestionBar.candidates.isEmpty)
        #expect(surface.toolbarView.clipboardChip.isHidden)
        #expect(surface.onSuggestionEdit == nil)
        #expect(!surface.isHidden)
    }

    /// Moving between two input views is not guaranteed to pass through a moment
    /// with no window, which is where touches were discarded before.
    @Test("A key held when the surface changes hands is let go")
    func resetDropsHeldKeys() throws {
        let surface = makeSurface()
        let key = try #require(surface.presentation.layout.rows.flatMap(\.keys).first { $0.role == .backspace })
        surface.interactionController.beginTouch(
            token: 1,
            key: key,
            point: CGPoint(x: 360, y: 220),
            sourceFrame: CGRect(x: 340, y: 198, width: 44, height: 44),
            containerBounds: surface.bounds,
            presentation: surface.presentation
        )
        #expect(surface.interactionController.activeTouchCount == 1)

        surface.resetForReuse()
        #expect(surface.interactionController.activeTouchCount == 0)
    }

    @Test("The host resets a surface it hands on, but not one it has just built")
    func hostResetsOnlyAHandedOnSurface() throws {
        let host = KeyboardSurfaceHost()
        let factory = SurfaceFactorySpy()
        let first = SurfaceClaimantFake()
        let second = SurfaceClaimantFake()
        host.claim(for: first, at: .appearing, make: factory.make)
        let surface = try #require(host.surface)
        surface.updateClipboardHint(.text)
        #expect(!surface.toolbarView.clipboardChip.isHidden)

        host.claim(for: second, at: .appearing, make: factory.make)
        #expect(surface.toolbarView.clipboardChip.isHidden)
    }

    @Test("Showing the same presentation again after a move builds no keys")
    func sameProjectionBuildsNothing() {
        let surface = makeSurface()
        let rebuilds = surface.keyRebuildCount
        surface.resetForReuse()
        surface.apply(presentation: surface.presentation, backgroundImage: nil)
        #expect(surface.keyRebuildCount == rebuilds)
    }

    private func makeSurface() -> KeyboardSurfaceView {
        let layout = StandardKeyboardLayouts.letters(.telex)
        let surface = KeyboardSurfaceView(presentation: KeyboardPresentation(layout: layout))
        surface.frame = CGRect(x: 0, y: 0, width: 390, height: 304)
        surface.layoutIfNeeded()
        return surface
    }
}
#endif
