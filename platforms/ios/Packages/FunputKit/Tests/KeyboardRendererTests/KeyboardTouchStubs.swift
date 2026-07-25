#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import UIKit

/// Fabricated touches for driving `KeyboardTouchOverlayView`'s UIResponder entry
/// points directly. Nothing here goes through UIKit's own dispatch, which is what
/// makes it work at all — synthesized touches cannot be delivered by the system.
final class StubTouch: UITouch {
    var stubPhase: UITouch.Phase = .began
    var stubLocation: CGPoint = .zero

    override var phase: UITouch.Phase { stubPhase }
    override func location(in view: UIView?) -> CGPoint { stubLocation }
}

final class StubTouchEvent: UIEvent {
    var stubTouches: Set<UITouch> = []
    var stubTimestamp: TimeInterval = 0

    override var allTouches: Set<UITouch>? { stubTouches }
    override var timestamp: TimeInterval { stubTimestamp }
}

/// Two keycaps and a log of everything the overlay reports upward.
@MainActor
final class OverlayTouchRecorder {
    static let keyAPoint = CGPoint(x: 10, y: 20)
    static let keyBPoint = CGPoint(x: 60, y: 20)

    let overlay = KeyboardTouchOverlayView(frame: CGRect(x: 0, y: 0, width: 100, height: 50))
    var log: [String] = []
    var lastReconcile: Set<KeyboardPressCommitQueue.TouchToken> = []

    init() {
        let a = KeySpec(id: "key-a", label: "a", role: .character)
        let b = KeySpec(id: "key-b", label: "b", role: .character)
        overlay.updateGeometry(
            ResolvedKeyboard(
                size: CGSize(width: 100, height: 50),
                toolbarFrame: nil,
                rows: [[
                    ResolvedKey(spec: a, frame: CGRect(x: 0, y: 0, width: 40, height: 40)),
                    ResolvedKey(spec: b, frame: CGRect(x: 50, y: 0, width: 40, height: 40)),
                ]]
            )
        )
        overlay.onBegin = { [weak self] token, hit, _ in
            self?.log.append("begin(\(token),\(hit.key.id))")
        }
        overlay.onMove = { [weak self] token, hit, _ in
            self?.log.append("move(\(token),\(hit?.key.id ?? "nil"))")
        }
        overlay.onEnd = { [weak self] in self?.log.append("end(\($0))") }
        overlay.onCancel = { [weak self] in self?.log.append("cancel(\($0))") }
        overlay.onReconcile = { [weak self] in self?.lastReconcile = $0 }
    }

    func touch(at point: CGPoint) -> StubTouch {
        let touch = StubTouch()
        touch.stubLocation = point
        return touch
    }

    /// Every callback carries the event, so `allTouches` is what UIKit currently
    /// knows — including fingers whose own callback has not been dispatched yet.
    private func event(_ touches: [UITouch], at timestamp: TimeInterval) -> StubTouchEvent {
        let event = StubTouchEvent()
        event.stubTouches = Set(touches)
        event.stubTimestamp = timestamp
        return event
    }

    func began(_ touch: StubTouch, alongside others: [UITouch] = [], at timestamp: TimeInterval) {
        overlay.touchesBegan([touch], with: event([touch] + others, at: timestamp))
    }

    func moved(_ touch: StubTouch, alongside others: [UITouch] = [], at timestamp: TimeInterval) {
        overlay.touchesMoved([touch], with: event([touch] + others, at: timestamp))
    }

    func ended(_ touch: StubTouch, alongside others: [UITouch] = [], at timestamp: TimeInterval) {
        overlay.touchesEnded([touch], with: event([touch] + others, at: timestamp))
    }
}
#endif
