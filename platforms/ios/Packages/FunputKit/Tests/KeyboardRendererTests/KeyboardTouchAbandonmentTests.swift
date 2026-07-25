#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import UIKit

/// What may count as an abandoned touch.
///
/// UIKit dispatches one callback per phase group of an event, so a finger that has
/// lifted already reports `.ended` while its own `touchesEnded` is still queued
/// behind the next finger's `touchesBegan`. Reading that as abandonment cancelled
/// the press — and a cancelled press produces no text — which dropped keys during
/// fast typing. These cases pin the boundary down.
@MainActor
struct KeyboardTouchAbandonmentTests {
    @Test("A finger lifting as the next one lands keeps its press")
    func liftedFingerKeepsItsPress() {
        let recorder = OverlayTouchRecorder()
        let first = recorder.touch(at: OverlayTouchRecorder.keyAPoint)
        recorder.began(first, at: 1)

        // Same event: the first finger has lifted, the second lands, and UIKit
        // happens to dispatch `began` before the first finger's `ended`.
        let second = recorder.touch(at: OverlayTouchRecorder.keyBPoint)
        first.stubPhase = .ended
        recorder.began(second, alongside: [first], at: 2)

        #expect(recorder.lastReconcile.contains(1), "the lifted finger is still tracked")
        recorder.ended(first, alongside: [second], at: 2)
        #expect(recorder.log == ["begin(1,key-a)", "begin(2,key-b)", "move(1,key-a)", "end(1)"])
    }

    @Test("A touch UIKit stops reporting is given up")
    func vanishedTouchIsAbandoned() {
        let recorder = OverlayTouchRecorder()
        let first = recorder.touch(at: OverlayTouchRecorder.keyAPoint)
        let second = recorder.touch(at: OverlayTouchRecorder.keyBPoint)
        recorder.began(first, at: 1)
        recorder.began(second, alongside: [first], at: 2)

        // The first finger's terminal callback never arrived and UIKit no longer
        // lists it, so nothing more is coming for it.
        recorder.moved(second, at: 3)

        #expect(recorder.lastReconcile == [2])
    }

    @Test("A finished touch is given up once a later event still reports it")
    func stuckFinishedTouchIsAbandoned() {
        let recorder = OverlayTouchRecorder()
        let touch = recorder.touch(at: OverlayTouchRecorder.keyAPoint)
        recorder.began(touch, at: 1)

        touch.stubPhase = .ended
        recorder.moved(touch, at: 2)
        #expect(recorder.lastReconcile == [1], "its callback may still be in flight")

        recorder.moved(touch, at: 3)
        #expect(recorder.lastReconcile.isEmpty, "a later event proves it is stuck")
    }

    @Test("A press on a recycled touch object is not swallowed")
    func recycledTouchObjectStillPresses() {
        let recorder = OverlayTouchRecorder()
        let touch = recorder.touch(at: OverlayTouchRecorder.keyAPoint)
        recorder.began(touch, at: 1)

        // UIKit recycles touch objects. If a mapping outlives its touch, the next
        // press landing on the same object must still register.
        touch.stubLocation = OverlayTouchRecorder.keyBPoint
        recorder.began(touch, at: 2)

        #expect(recorder.log == ["begin(1,key-a)", "cancel(1)", "begin(2,key-b)"])
    }

    @Test("A toolbar press is not mistaken for an abandoned touch")
    func toolbarPressSurvivesReconcile() {
        var events: [KeyboardKeyEvent] = []
        let controller = KeyboardSurfaceInteractionController(
            onEvent: { events.append($0) },
            onPreview: { _, _ in }
        )
        var presentation = KeyboardPresentation()
        presentation.isHapticFeedbackEnabled = false
        let emoji = KeySpec(id: "key-emoji", label: "☺", role: .emoji)

        controller.handle(
            KeyboardKeyEvent(key: emoji, phase: .pressed),
            sourceFrame: .zero,
            presentation: presentation
        )
        // A keycap touch elsewhere reports only the overlay's own tokens.
        controller.reconcileActiveTouches([])
        controller.handle(
            KeyboardKeyEvent(key: emoji, phase: .released),
            sourceFrame: .zero,
            presentation: presentation
        )

        #expect(events.map(\.phase) == [.pressed, .released])
    }
}
#endif
