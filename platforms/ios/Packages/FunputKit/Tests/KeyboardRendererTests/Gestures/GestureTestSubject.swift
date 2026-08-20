#if canImport(UIKit)
@testable import KeyboardRenderer
import Foundation
import KeyboardLayout
import UIKit

/// Scheduler that keeps every pending timer, not just the last one.
///
/// The spacebar arms two at once — the trackpad hold and the repeat — so the single-slot
/// `TestRepeatScheduler` cannot drive it.
@MainActor
final class TestGestureScheduler {
    private var pending: [(delay: TimeInterval, action: @MainActor () -> Void)] = []

    func schedule(
        delay: TimeInterval,
        action: @escaping @MainActor () -> Void
    ) -> KeyboardScheduledAction {
        let entry = (delay: delay, action: action)
        pending.append(entry)
        let index = pending.count - 1
        return KeyboardScheduledAction { [weak self] in
            guard let self, pending.indices.contains(index) else { return }
            pending[index].action = {}
        }
    }

    /// Runs the earliest pending timer with the given delay, if any.
    func fire(after delay: TimeInterval) {
        guard let index = pending.firstIndex(where: { $0.delay == delay }) else { return }
        let action = pending[index].action
        pending.remove(at: index)
        action()
    }
}

/// One interaction controller over a single key, driven by explicit touch points.
@MainActor
final class GestureTestSubject {
    let scheduler = TestGestureScheduler()
    var events: [(token: UInt64, event: KeyboardKeyEvent)] = []
    var claims: [KeyboardSurfaceInteractionController.GestureClaim] = []
    var grantsClaims = true
    let key: KeySpec
    var smartGestures = true

    lazy var controller = KeyboardSurfaceInteractionController(
        onEvent: { _ in },
        onContactEvent: { [weak self] token, event in
            self?.events.append((token, event))
        },
        onClaimGesture: { [weak self] _, kind in
            guard let self else { return false }
            claims.append(kind)
            return grantsClaims
        },
        onPreview: { _, _ in },
        repeatScheduler: scheduler.schedule
    )

    init(key: KeySpec) {
        self.key = key
    }

    var phases: [KeyboardKeyEvent.Phase] { events.map(\.event.phase) }

    var presentation: KeyboardPresentation {
        var value = KeyboardPresentation()
        value.isHapticFeedbackEnabled = false
        value.areSmartGesturesEnabled = smartGestures
        return value
    }

    func begin(at x: CGFloat = 120) {
        controller.beginTouch(
            token: 1,
            key: key,
            point: CGPoint(x: x, y: 220),
            sourceFrame: CGRect(x: 102, y: 198, width: 36, height: 44),
            containerBounds: CGRect(x: 0, y: 0, width: 390, height: 304),
            presentation: presentation
        )
    }

    func move(to x: CGFloat, y: CGFloat = 220) {
        controller.moveTouch(
            token: 1,
            key: key,
            point: CGPoint(x: x, y: y),
            sourceFrame: CGRect(x: 102, y: 198, width: 36, height: 44),
            presentation: presentation
        )
    }
}
#endif
