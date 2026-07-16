import KeyboardLayout
import KeyboardRenderer
import Testing
import UIKit

@MainActor
struct KeyboardRolloverStressTests {
    @Test("Keyboard surface sustains overlapping touches without dropping releases")
    func sustainedRollover() throws {
        var presentation = KeyboardPresentation()
        presentation.isHapticFeedbackEnabled = false
        presentation.showsKeyPreviews = false
        let surface = KeyboardSurfaceView(presentation: presentation)
        #expect(surface.isMultipleTouchEnabled)
        var events: [KeyboardKeyEvent] = []
        surface.onKeyEvent = { events.append($0) }

        let controls = descendantControls(in: surface).filter {
            $0.allControlEvents.contains(.touchDown)
                && $0.allControlEvents.contains(.touchUpInside)
        }
        let first = try #require(controls.first)
        let second = try #require(controls.dropFirst().first)

        for index in 0..<1_000 {
            first.sendActions(for: .touchDown)
            second.sendActions(for: .touchDown)
            if index.isMultiple(of: 2) {
                first.sendActions(for: .touchUpInside)
                second.sendActions(for: .touchUpInside)
            } else {
                second.sendActions(for: .touchUpInside)
                first.sendActions(for: .touchUpInside)
            }
        }

        let pressedIDs = events
            .filter { $0.phase == .pressed }
            .map(\.key.id)
        let releasedIDs = events
            .filter { $0.phase == .released }
            .map(\.key.id)
        #expect(releasedIDs.count == 2_000)
        #expect(releasedIDs.elementsEqual(pressedIDs))
        #expect(!events.contains { $0.phase == .cancelled })
    }

    @Test("Cancelling the first touch releases a completed rollover successor")
    func cancellationUnblocksSuccessor() throws {
        var presentation = KeyboardPresentation()
        presentation.isHapticFeedbackEnabled = false
        presentation.showsKeyPreviews = false
        let surface = KeyboardSurfaceView(presentation: presentation)
        var events: [KeyboardKeyEvent] = []
        surface.onKeyEvent = { events.append($0) }

        let controls = descendantControls(in: surface).filter {
            $0.allControlEvents.contains(.touchDown)
                && $0.allControlEvents.contains(.touchUpInside)
        }
        let first = try #require(controls.first)
        let second = try #require(controls.dropFirst().first)

        first.sendActions(for: .touchDown)
        second.sendActions(for: .touchDown)
        second.sendActions(for: .touchUpInside)
        #expect(events.filter { $0.phase == .released }.isEmpty)
        first.sendActions(for: .touchCancel)

        #expect(events.suffix(2).map(\.phase) == [.cancelled, .released])
        let presses = events.filter { $0.phase == .pressed }.map(\.key.id)
        #expect(events.suffix(2).map(\.key.id) == presses)
    }

    private func descendantControls(in view: UIView) -> [UIControl] {
        view.subviews.flatMap { child in
            let own = (child as? UIControl).map { [$0] } ?? []
            return own + descendantControls(in: child)
        }
    }
}
