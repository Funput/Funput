#if canImport(UIKit)
@testable import KeyboardRenderer
import CoreGraphics
import Foundation
import KeyboardLayout
import Testing
import UIKit

/// A layout swap tears down presentation, not contacts.
///
/// Tapping `?123` while another finger still rests on a keycap used to clear the whole
/// identity map, so that finger's release arrived with no `ContactID` and was dropped in
/// silence. The surface keeps a per-contact geometry snapshot precisely so a press that
/// began under the old layout can still commit to the key it landed on.
@MainActor
struct KeyboardTouchContinuityTests {
    @Test("A layout swap keeps a finger that is still down")
    func layoutSwapKeepsPendingContact() throws {
        let surface = makeSurface()
        let events = EventBox()
        surface.onKeyEvent = { events.values.append($0) }

        let touch = ContinuityStubTouch()
        touch.stubLocation = try center(of: "character-a", in: surface)
        surface.touchOverlay.touchesBegan([touch], with: nil)

        surface.presentation.layout = KeyboardLayoutResolver.resolve(
            inputMethod: .vni,
            mode: .symbolsPrimary
        )
        surface.layoutIfNeeded()

        surface.touchOverlay.touchesEnded([touch], with: nil)

        #expect(events.released.map(\.key.id) == ["character-a"])
        #expect(surface.touchCoordinator.metrics.contactsAbandoned == 0)
    }

    /// The mirror case: when the keyboard itself goes away the proxy goes with it, so an
    /// unfinished touch must not commit. Guards against "fixing" the above by never
    /// tearing anything down.
    @Test("Losing the window still discards a finger that never lifted")
    func windowLossDiscardsPendingContact() throws {
        let surface = makeSurface()
        let events = EventBox()
        surface.onKeyEvent = { events.values.append($0) }

        let touch = ContinuityStubTouch()
        touch.stubLocation = try center(of: "character-a", in: surface)
        surface.touchOverlay.touchesBegan([touch], with: nil)

        surface.didMoveToWindow()
        surface.touchOverlay.touchesEnded([touch], with: nil)

        #expect(events.released.isEmpty)
    }

    private func makeSurface() -> KeyboardSurfaceView {
        let surface = KeyboardSurfaceView(
            presentation: KeyboardPresentation(
                layout: KeyboardLayoutResolver.resolve(inputMethod: .vni, mode: .letters)
            )
        )
        surface.frame = CGRect(x: 0, y: 0, width: 390, height: 304)
        surface.layoutIfNeeded()
        return surface
    }

    private func center(
        of keyID: String,
        in surface: KeyboardSurfaceView
    ) throws -> CGPoint {
        let geometry = try #require(surface.touchCoordinator.geometrySnapshot?.geometry)
        let key = try #require(geometry.keys.first { $0.spec.id == keyID })
        return CGPoint(x: key.frame.midX, y: key.frame.midY)
    }
}

@MainActor
private final class EventBox {
    var values: [KeyboardKeyEvent] = []
    var released: [KeyboardKeyEvent] { values.filter { $0.phase == .released } }
}

private final class ContinuityStubTouch: UITouch {
    nonisolated(unsafe) var stubLocation: CGPoint = .zero

    override var timestamp: TimeInterval { 1 }
    override func location(in view: UIView?) -> CGPoint { stubLocation }
    override func previousLocation(in view: UIView?) -> CGPoint { stubLocation }
}
#endif
