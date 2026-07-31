#if canImport(UIKit)
@testable import KeyboardRenderer
import CoreGraphics
import Foundation
import KeyboardLayout
import KeyboardTouchCore
import KeyboardTouchUIKit
import Testing
import UIKit

/// The overlay runs one UIKit callback as recognize → commit → finish. A swipe crosses its
/// threshold on the final move, so recognition must see that move before the pipeline commits
/// the key underneath; otherwise the spacebar inserts a space and the gesture is lost.
@MainActor
struct KeyboardTouchOverlayOrderTests {
    @Test("Recognition sees the final move before the pipeline commits")
    func recognitionPrecedesCommit() {
        let overlay = KeyboardTouchOverlayView()
        overlay.frame = CGRect(x: 0, y: 0, width: 100, height: 50)
        overlay.adoptGeometry(
            KeyboardGeometrySnapshot(revision: 1, geometry: spacebarGeometry())
        )
        let trace = Trace()
        overlay.onBegin = { _, _, _ in trace.steps.append("begin") }
        overlay.onMove = { _, _, _ in trace.steps.append("move") }
        overlay.onEnd = { _ in trace.steps.append("end") }
        overlay.onCancel = { _ in trace.steps.append("cancel") }
        overlay.onSamples = { _ in trace.steps.append("commit") }

        let touch = OverlayStubTouch()
        touch.stubLocation = CGPoint(x: 25, y: 25)
        overlay.touchesBegan([touch], with: nil)
        touch.stubLocation = CGPoint(x: 70, y: 25)
        overlay.touchesEnded([touch], with: nil)

        #expect(trace.steps == ["begin", "commit", "move", "commit", "end"])
    }

    @Test("A cancellation reaches the controller only after the pipeline saw it")
    func cancellationFollowsCommit() {
        let overlay = KeyboardTouchOverlayView()
        overlay.frame = CGRect(x: 0, y: 0, width: 100, height: 50)
        overlay.adoptGeometry(
            KeyboardGeometrySnapshot(revision: 1, geometry: spacebarGeometry())
        )
        let trace = Trace()
        overlay.onBegin = { _, _, _ in trace.steps.append("begin") }
        overlay.onMove = { _, _, _ in trace.steps.append("move") }
        overlay.onCancel = { _ in trace.steps.append("cancel") }
        overlay.onSamples = { _ in trace.steps.append("commit") }

        let touch = OverlayStubTouch()
        touch.stubLocation = CGPoint(x: 25, y: 25)
        overlay.touchesBegan([touch], with: nil)
        overlay.touchesCancelled([touch], with: nil)

        #expect(trace.steps == ["begin", "commit", "commit", "cancel"])
    }

    private func spacebarGeometry() -> ResolvedKeyboard {
        ResolvedKeyboard(
            size: CGSize(width: 100, height: 50),
            toolbarFrame: nil,
            rows: [[ResolvedKey(
                spec: KeySpec(
                    id: "space",
                    label: "space",
                    role: .space,
                    horizontalSwipeAction: .toggleLanguage
                ),
                frame: CGRect(x: 0, y: 0, width: 100, height: 50)
            )]]
        )
    }
}

@MainActor
private final class Trace {
    var steps: [String] = []
}

private final class OverlayStubTouch: UITouch {
    nonisolated(unsafe) var stubLocation: CGPoint = .zero

    override var timestamp: TimeInterval { 1 }
    override func location(in view: UIView?) -> CGPoint { stubLocation }
    override func previousLocation(in view: UIView?) -> CGPoint { stubLocation }
}
#endif
