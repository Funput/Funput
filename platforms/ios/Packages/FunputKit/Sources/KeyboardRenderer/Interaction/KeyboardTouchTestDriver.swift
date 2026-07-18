#if canImport(UIKit)
import KeyboardLayout
import UIKit

/// Release-build unit-test seam. SPI keeps touch internals out of the normal
/// KeyboardRenderer API while allowing the app's native test target to drive
/// the exact production controller.
@_spi(Testing)
@MainActor
public final class KeyboardTouchTestDriver {
    private final class EventBox {
        var values: [KeyboardKeyEvent] = []
    }

    @MainActor
    private final class CancellationBox {
        var isCancelled = false
    }

    @MainActor
    private final class ManualRepeatScheduler {
        private var pending: [(CancellationBox, @MainActor @Sendable () -> Void)] = []

        func schedule(
            _ delay: TimeInterval,
            _ action: @escaping @MainActor @Sendable () -> Void
        ) -> KeyboardScheduledAction {
            _ = delay
            let cancellation = CancellationBox()
            pending.append((cancellation, action))
            return KeyboardScheduledAction { cancellation.isCancelled = true }
        }

        func runNext() {
            guard !pending.isEmpty else { return }
            let (cancellation, action) = pending.removeFirst()
            if !cancellation.isCancelled { action() }
        }
    }

    private let box: EventBox
    private let repeatScheduler: ManualRepeatScheduler
    private let controller: KeyboardSurfaceInteractionController
    private let presentation: KeyboardPresentation

    public var events: [KeyboardKeyEvent] { box.values }
    public var queueDepth: Int { controller.queueDepth }

    public init() {
        let box = EventBox()
        let repeatScheduler = ManualRepeatScheduler()
        var presentation = KeyboardPresentation()
        presentation.isHapticFeedbackEnabled = false
        presentation.isKeySoundEnabled = false
        presentation.showsKeyPreviews = false
        self.box = box
        self.repeatScheduler = repeatScheduler
        self.presentation = presentation
        controller = KeyboardSurfaceInteractionController(
            onEvent: { box.values.append($0) },
            onPreview: { _, _ in },
            repeatScheduler: { delay, action in
                repeatScheduler.schedule(delay, action)
            }
        )
    }

    public func begin(token: UInt64, key: KeySpec, point: CGPoint = .zero) {
        controller.beginTouch(
            token: token,
            key: key,
            point: point,
            sourceFrame: nil,
            presentation: presentation
        )
    }

    public func move(token: UInt64, key: KeySpec?, point: CGPoint) {
        controller.moveTouch(
            token: token,
            key: key,
            point: point,
            sourceFrame: nil,
            presentation: presentation
        )
    }

    public func end(token: UInt64) {
        controller.endTouch(token: token)
    }

    public func runNextRepeat() {
        repeatScheduler.runNext()
    }

    public func reconcile(activeTokens: Set<UInt64>) {
        controller.reconcileActiveTouches(activeTokens)
    }

    public func performAccessibilitySwipe(key: KeySpec, action: KeySwipeAction) {
        controller.handle(
            .init(key: key, phase: .swiped(action)),
            sourceFrame: nil,
            presentation: presentation
        )
    }

    public static func hitKey(
        in geometry: ResolvedKeyboard,
        at point: CGPoint
    ) -> KeySpec? {
        let overlay = KeyboardTouchOverlayView()
        overlay.updateGeometry(geometry)
        return overlay.resolvedHit(at: point)?.key
    }
}
#endif
